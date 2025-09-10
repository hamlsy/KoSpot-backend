package com.kospot.infrastructure.websocket.domain.gameroom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.domain.multigame.gamePlayer.exception.GameTeamErrorStatus;
import com.kospot.domain.multigame.gamePlayer.exception.GameTeamHandler;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomPlayerInfo;
import com.kospot.domain.multigame.gameRoom.vo.RoomPlayerStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 게임방 Redis 데이터 관리 서비스
 * Redis 기반 플레이어 정보, 인원 수, 강퇴 목록 등을 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomRedisService {

    // Redis Key Patterns
    private static final String ROOM_PLAYERS_KEY = "game:room:%s:players";
    private static final String ROOM_BANNED_KEY = "game:room:%s:banned";
    private static final String PLAYER_SESSION_KEY = "game:player:%s:session";
    private static final String SESSION_SUBSCRIPTIONS_KEY = "game:session:%s:subscriptions";
    private static final String SESSION_ROOM_KEY = "game:session:%s:room";

    // Expiry Settings
    private static final int SESSION_EXPIRY_HOURS = 24;
    private static final int ROOM_DATA_EXPIRY_HOURS = 12;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 게임방에 플레이어 정보 저장
     */
    public void addPlayerToRoom(String roomId, GameRoomPlayerInfo playerInfo) {
        try {
            String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);
            String playerJson = objectMapper.writeValueAsString(playerInfo);

            //todo implement member detail statistic info in http methods -> 그냥 http 요청으로 처리, redis에 너무 많은 데이터
            redisTemplate.opsForHash().put(roomKey, playerInfo.getMemberId().toString(), playerJson);
            redisTemplate.expire(roomKey, ROOM_DATA_EXPIRY_HOURS, TimeUnit.HOURS);

            log.debug("Added player to room Redis - RoomId: {}, PlayerId: {}", roomId, playerInfo.getMemberId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize player info for Redis - RoomId: {}, PlayerId: {}",
                    roomId, playerInfo.getMemberId(), e);
            throw new RuntimeException("플레이어 정보 저장 실패", e);
        }
    }

    /**
     * 게임방에서 플레이어 정보 제거
     */
    public GameRoomPlayerInfo removePlayerFromRoom(String roomId, Long memberId) {
        try {
            String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);
            String playerJson = (String) redisTemplate.opsForHash().get(roomKey, memberId.toString());

            if (playerJson != null) {
                GameRoomPlayerInfo playerInfo = objectMapper.readValue(playerJson, GameRoomPlayerInfo.class);
                redisTemplate.opsForHash().delete(roomKey, memberId.toString());

                log.debug("Removed player from room Redis - RoomId: {}, PlayerId: {}", roomId, memberId);
                return playerInfo;
            }

            return null;

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize player info from Redis - RoomId: {}, PlayerId: {}",
                    roomId, memberId, e);
            return null;
        }
    }

    /**
     * 게임방의 현재 플레이어 목록 조회
     */
    public List<GameRoomPlayerInfo> getRoomPlayers(String roomId) {
        try {
            String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);
            Map<Object, Object> players = redisTemplate.opsForHash().entries(roomKey);

            return players.values().stream()
                    .map(playerJson -> {
                        try {
                            return objectMapper.readValue((String) playerJson, GameRoomPlayerInfo.class);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to deserialize player info: {}", playerJson);
                            return null;
                        }
                    })
                    .filter(player -> player != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get room players from Redis - RoomId: {}", roomId, e);
            return List.of();
        }
    }

    public void switchTeam(String roomId, Long memberId, String newTeam) {
        try {
            if(isNotValidTeamCount(roomId, newTeam)) {
                log.warn("Team switch denied - RoomId: {}, PlayerId: {}, NewTeam: {} (Team full)",
                        roomId, memberId, newTeam);
                return;
            }
            String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);
            String playerJson = (String) redisTemplate.opsForHash().get(roomKey, memberId.toString());

            if (playerJson != null) {
                GameRoomPlayerInfo playerInfo = objectMapper.readValue(playerJson, GameRoomPlayerInfo.class);
                playerInfo.setTeam(newTeam);

                String updatedJson = objectMapper.writeValueAsString(playerInfo);
                redisTemplate.opsForHash().put(roomKey, memberId.toString(), updatedJson);

            }

        } catch (Exception e) {
            log.error("Failed to switch player team in Redis - RoomId: {}, PlayerId: {}, NewTeam: {}",
                    roomId, memberId, newTeam, e);
        }
    }

    private boolean isNotValidTeamCount(String roomId, String team) {
        int count = getTeamCount(roomId, team);
        return count >= 4;
    }

    private int getTeamCount(String roomId, String team) {
        List<GameRoomPlayerInfo> playerInfoList = getRoomPlayers(roomId);
        return (int) playerInfoList.stream()
                .filter(player -> team.equals(player.getTeam()))
                .count();
    }

    //todo refactor
    public void assignAllPlayersTeam(String roomId) {
        String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);
        List<GameRoomPlayerInfo> players = getRoomPlayers(roomKey);
        int totalPlayers = players.size();
        int redTeamSize = (totalPlayers + 1) / 2;
        Map<Object, Object> teamAssignments = new HashMap<>();


        for(int i = 0; i < totalPlayers; i++) {
            GameRoomPlayerInfo player = players.get(i);
            String team = (i < redTeamSize) ? "RED" : "BLUE";
            player.setTeam(team);
            try {
                teamAssignments.put(player.getMemberId().toString(), objectMapper.writeValueAsString(player));
            }catch (JsonProcessingException e) {
                log.error("Failed to serialize player info for Redis during team assignment - RoomId: {}, PlayerId: {}",
                        roomId, player.getMemberId(), e);
                throw new GameTeamHandler(GameTeamErrorStatus.GAME_TEAM_ERROR_UNKNOWN);
            }
        }
        if(!teamAssignments.isEmpty()) {
            redisTemplate.opsForHash().putAll(roomKey, teamAssignments);
        }

    }

    /**
     * 게임방 현재 인원 수 조회 (Redis 기반)
     */
    public int getCurrentPlayerCount(String roomId) {
        try {
            String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);
            Long count = redisTemplate.opsForHash().size(roomKey);
            return count.intValue();
        } catch (Exception e) {
            log.error("Failed to get current player count from Redis - RoomId: {}", roomId, e);
            return 0;
        }
    }

    /**
     * 게임방이 비어있는지 확인
     */
    public boolean isRoomEmpty(String roomId) {
        return getCurrentPlayerCount(roomId) == 0;
    }

    /**
     *  "Read-Then-Check"
     *  race condition 방지 안 됨 todo refactor
     */
    public boolean cannotJoinRoom(String roomId, int maxPlayers) {
        int currentCount = getCurrentPlayerCount(roomId);
        boolean canJoin = currentCount < maxPlayers;

        log.debug("Room capacity check - RoomId: {}, Current: {}, Max: {}, CanJoin: {}",
                roomId, currentCount, maxPlayers, canJoin);

        return !canJoin;
    }


    /**
     * 게임방 통계 정보 조회
     */
    public RoomPlayerStats getRoomPlayerStats(String roomId) {
        int currentCount = getCurrentPlayerCount(roomId);
        Set<String> bannedPlayers = redisTemplate.opsForSet().members(String.format(ROOM_BANNED_KEY, roomId));
        int bannedCount = bannedPlayers != null ? bannedPlayers.size() : 0;

        return RoomPlayerStats.builder()
                .roomId(roomId)
                .currentPlayerCount(currentCount)
                .lastUpdated(System.currentTimeMillis())
                .build();
    }

    /**
     * 세션 정보 저장
     */
    public void saveSessionInfo(String sessionId, String roomId, String destination, Long memberId) {
        // 세션의 구독 정보 저장
        String sessionKey = String.format(SESSION_SUBSCRIPTIONS_KEY, sessionId);
        redisTemplate.opsForSet().add(sessionKey, destination);
        redisTemplate.expire(sessionKey, SESSION_EXPIRY_HOURS, TimeUnit.HOURS);

        // 플레이어 세션 매핑
        String playerSessionKey = String.format(PLAYER_SESSION_KEY, memberId);
        redisTemplate.opsForValue().set(playerSessionKey, sessionId, SESSION_EXPIRY_HOURS, TimeUnit.HOURS);

        // 세션-룸 매핑
        String sessionRoomKey = String.format(SESSION_ROOM_KEY, sessionId);
        redisTemplate.opsForValue().set(sessionRoomKey, roomId, SESSION_EXPIRY_HOURS, TimeUnit.HOURS);
    }

    /**
     * 세션에서 룸 ID 조회
     */
    public String getRoomIdFromSession(String sessionId) {
        String sessionRoomKey = String.format(SESSION_ROOM_KEY, sessionId);
        return redisTemplate.opsForValue().get(sessionRoomKey);
    }

    /**
     * 세션에서 멤버 ID 조회
     */
    public Long getMemberIdFromSession(String sessionId) {
        try {
            Set<String> playerKeys = redisTemplate.keys(String.format(PLAYER_SESSION_KEY, "*"));
            if (playerKeys != null) {
                for (String playerKey : playerKeys) {
                    String storedSessionId = redisTemplate.opsForValue().get(playerKey);
                    if (sessionId.equals(storedSessionId)) {
                        // 키에서 멤버 ID 추출: game:player:{memberId}:session
                        String[] parts = playerKey.split(":");
                        return Long.parseLong(parts[2]);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get member ID from session: {}", sessionId, e);
            return null;
        }
    }

    /**
     * 플레이어 세션 정보 정리
     */
    public void cleanupPlayerSession(Long memberId) {
        try {
            String playerSessionKey = String.format(PLAYER_SESSION_KEY, memberId);
            String sessionId = redisTemplate.opsForValue().get(playerSessionKey);

            if (sessionId != null) {
                String sessionSubscriptionsKey = String.format(SESSION_SUBSCRIPTIONS_KEY, sessionId);
                String sessionRoomKey = String.format(SESSION_ROOM_KEY, sessionId);

                redisTemplate.delete(playerSessionKey);
                redisTemplate.delete(sessionSubscriptionsKey);
                redisTemplate.delete(sessionRoomKey);

                log.debug("Cleaned up player session - MemberId: {}, SessionId: {}", memberId, sessionId);
            }
        } catch (Exception e) {
            log.error("Failed to cleanup player session - MemberId: {}", memberId, e);
        }
    }

    /**
     * 비어있는 게임방의 Redis 데이터 정리
     */
    @Async("taskExecutor")
    public void cleanupEmptyRooms() {
        try {
            Set<String> roomKeys = redisTemplate.keys(String.format(ROOM_PLAYERS_KEY, "*"));
            int cleanedRooms = 0;

            if (roomKeys != null) {
                for (String key : roomKeys) {
                    String roomId = extractRoomIdFromKey(key);

                    if (isRoomEmpty(roomId)) {
                        // 플레이어 목록 삭제
                        redisTemplate.delete(key);

                        // 강퇴 목록 삭제
                        String bannedKey = String.format(ROOM_BANNED_KEY, roomId);
                        redisTemplate.delete(bannedKey);

                        cleanedRooms++;
                        log.debug("Cleaned up empty room - RoomId: {}", roomId);
                    }
                }
            }

            log.info("Redis cleanup completed - {} empty rooms cleaned", cleanedRooms);

        } catch (Exception e) {
            log.error("Failed to cleanup empty rooms in Redis", e);
        }
    }

    /**
     * 게임방 통계 로깅
     */
    public void logRoomStatistics() {
        try {
            Set<String> roomKeys = redisTemplate.keys(String.format(ROOM_PLAYERS_KEY, "*"));

            if (roomKeys == null || roomKeys.isEmpty()) {
                log.info("Game Room Redis Statistics - No active rooms");
                return;
            }

            int totalRooms = roomKeys.size();
            int totalPlayers = 0;
            int emptyRooms = 0;

            for (String key : roomKeys) {
                String roomId = extractRoomIdFromKey(key);
                int playerCount = getCurrentPlayerCount(roomId);

                totalPlayers += playerCount;
                if (playerCount == 0) {
                    emptyRooms++;
                }
            }

            log.info("Game Room Redis Statistics - Total Rooms: {}, Total Players: {}, Empty Rooms: {}, Avg Players/Room: {}",
                    totalRooms, totalPlayers, emptyRooms,
                    totalRooms > 0 ? (double) totalPlayers / totalRooms : 0);

        } catch (Exception e) {
            log.error("Failed to generate room statistics from Redis", e);
        }
    }

    /**
     * 활성 게임방 키 목록 조회
     */
    public Set<String> getActiveRoomKeys() {
        return redisTemplate.keys(String.format(ROOM_PLAYERS_KEY, "*"));
    }

    /**
     * Redis 키에서 룸 ID 추출
     */
    private String extractRoomIdFromKey(String key) {
        // game:room:{roomId}:players -> {roomId} 추출
        String[] parts = key.split(":");
        return parts.length >= 3 ? parts[2] : null;
    }
} 