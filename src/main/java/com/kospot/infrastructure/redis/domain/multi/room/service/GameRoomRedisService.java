package com.kospot.infrastructure.redis.domain.multi.room.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.domain.multi.gamePlayer.exception.GameTeamErrorStatus;
import com.kospot.domain.multi.gamePlayer.exception.GameTeamHandler;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.redis.common.service.SessionContextRedisService;
import com.kospot.infrastructure.redis.domain.multi.room.dao.GameRoomRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomRedisService {

    // Redis Key Patterns
    private static final String ROOM_PLAYERS_KEY = "game:room:%s:players";
    private static final String PLAYER_SESSION_KEY = "game:player:%s:session";
    private static final String SESSION_SUBSCRIPTIONS_KEY = "game:session:%s:subscriptions";
    private static final String SESSION_ROOM_KEY = "game:session:%s:room";

    // Expiry Settings
    private static final int SESSION_EXPIRY_HOURS = 24;
    private static final int ROOM_DATA_EXPIRY_HOURS = 12;

    private final GameRoomRedisRepository gameRoomRedisRepository;
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
            gameRoomRedisRepository.savePlayer(roomKey, playerInfo.getMemberId().toString(), playerJson, ROOM_DATA_EXPIRY_HOURS);

            log.debug("Added player to room Redis - RoomId: {}, PlayerId: {}", roomId, playerInfo.getMemberId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize player info for Redis - RoomId: {}, PlayerId: {}",
                    roomId, playerInfo.getMemberId(), e);
            throw new RuntimeException("플레이어 정보 저장 실패", e);
        }
    }

    public GameRoomPlayerInfo removePlayerFromRoom(String roomId, Long memberId) {
        try {
            String roomKey = getRoomKey(roomId);
            String playerJson = gameRoomRedisRepository.findPlayer(roomKey, memberId.toString());

            if (playerJson != null) {
                GameRoomPlayerInfo playerInfo = objectMapper.readValue(playerJson, GameRoomPlayerInfo.class);
                gameRoomRedisRepository.deletePlayer(roomKey, memberId.toString());

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

    public List<GameRoomPlayerInfo> getRoomPlayers(String roomId) {
        try {
            String roomKey = getRoomKey(roomId);
            Map<Object, Object> players = gameRoomRedisRepository.findAllPlayers(roomKey);

            return players.values().stream()
                    .map(playerJson ->
                            {
                                try {
                                    return objectMapper.readValue((String) playerJson, GameRoomPlayerInfo.class);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                    )
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get room players from Redis - RoomId: {}", roomId, e);
            return List.of();
        }
    }

    public void switchTeam(String roomId, Long memberId, String newTeam) {
        try {
            if (isNotValidTeamCount(roomId, newTeam)) {
                log.warn("Team switch denied - RoomId: {}, PlayerId: {}, NewTeam: {} (Team full)",
                        roomId, memberId, newTeam);
                return;
            }
            String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);
            String playerJson = gameRoomRedisRepository.findPlayer(roomKey, memberId.toString());

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
        List<GameRoomPlayerInfo> players = getRoomPlayers(roomId);
        int totalPlayers = players.size();
        int redTeamSize = (totalPlayers + 1) / 2;
        Map<Object, Object> teamAssignments = new HashMap<>();

        for (int i = 0; i < totalPlayers; i++) {
            GameRoomPlayerInfo player = players.get(i);
            String team = (i < redTeamSize) ? "RED" : "BLUE";
            player.setTeam(team);
            try {
                teamAssignments.put(player.getMemberId().toString(), objectMapper.writeValueAsString(player));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize player info for Redis during team assignment - RoomId: {}, PlayerId: {}",
                        roomId, player.getMemberId(), e);
                throw new GameTeamHandler(GameTeamErrorStatus.GAME_TEAM_ERROR_UNKNOWN);
            }
        }
        if (!teamAssignments.isEmpty()) {
            redisTemplate.opsForHash().putAll(roomKey, teamAssignments);
        }

    }

    //todo refactor
    public void resetAllPlayersTeam(String roomId) {
        String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);
        List<GameRoomPlayerInfo> players = getRoomPlayers(roomId);
        Map<Object, Object> teamRemovals = new HashMap<>();

        for (GameRoomPlayerInfo player : players) {
            player.setTeam(null);
            try {
                teamRemovals.put(player.getMemberId().toString(), objectMapper.writeValueAsString(player));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize player info for Redis during team removal - RoomId: {}, PlayerId: {}",
                        roomId, player.getMemberId(), e);
                throw new GameTeamHandler(GameTeamErrorStatus.GAME_TEAM_ERROR_UNKNOWN);
            }
        }
        if (!teamRemovals.isEmpty()) {
            redisTemplate.opsForHash().putAll(roomKey, teamRemovals);
        }
    }

    public boolean isRoomEmpty(String roomId) {
        String roomKey = getRoomKey(roomId);
        int currentPlayerCount = gameRoomRedisRepository.countPlayers(roomKey);
        return currentPlayerCount == 0;
    }

    private String getRoomKey(String roomId) {
        return String.format(ROOM_PLAYERS_KEY, roomId);
    }

    /**
     * race condition 방지 안 됨 todo refactor
     */
    public boolean cannotJoinRoom(String roomId, int maxPlayers) {
        String roomKey = getRoomKey(roomId);
        int currentCount = gameRoomRedisRepository.countPlayers(roomKey);
        boolean canJoin = currentCount < maxPlayers;

        log.debug("Room capacity check - RoomId: {}, Current: {}, Max: {}, CanJoin: {}",
                roomId, currentCount, maxPlayers, canJoin);

        return !canJoin;
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

    public String getRoomIdFromSession(String sessionId) {
        String sessionRoomKey = String.format(SESSION_ROOM_KEY, sessionId);
        return redisTemplate.opsForValue().get(sessionRoomKey);
    }

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

    public Set<String> getActiveRoomKeys() {
        return redisTemplate.keys(String.format(ROOM_PLAYERS_KEY, "*"));
    }

} 