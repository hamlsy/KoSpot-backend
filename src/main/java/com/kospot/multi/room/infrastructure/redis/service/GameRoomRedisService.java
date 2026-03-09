package com.kospot.multi.room.infrastructure.redis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


import com.kospot.multi.player.domain.exception.GameTeamErrorStatus;
import com.kospot.multi.player.domain.exception.GameTeamHandler;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import com.kospot.multi.room.domain.vo.MultiplayerScreenState;
import com.kospot.multi.room.infrastructure.redis.dao.GameRoomRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
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

    public enum ScreenStateUpdateStatus {
        UPDATED,
        NO_OP,
        STALE,
        NOT_FOUND
    }

    public record ScreenStateUpdateResult(ScreenStateUpdateStatus status, GameRoomPlayerInfo playerInfo) {
        public static ScreenStateUpdateResult of(ScreenStateUpdateStatus status, GameRoomPlayerInfo playerInfo) {
            return new ScreenStateUpdateResult(status, playerInfo);
        }

        public static ScreenStateUpdateResult of(ScreenStateUpdateStatus status) {
            return new ScreenStateUpdateResult(status, null);
        }
    }

    /**
     * 게임방에 플레이어 정보 저장
     */
    public void savePlayerToRoom(String roomId, GameRoomPlayerInfo playerInfo) {
        try {
            String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);
            String playerJson = objectMapper.writeValueAsString(playerInfo);
            gameRoomRedisRepository.savePlayer(roomKey, playerInfo.getMemberId().toString(), playerJson, ROOM_DATA_EXPIRY_HOURS);

            log.debug("Added player to room Redis - RoomId: {}, PlayerId: {}", roomId, playerInfo.getMemberId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize player info for Redis - RoomId: {}, PlayerId: {}",
                    roomId, playerInfo.getMemberId(), e);
            throw new RuntimeException("플레이어 정보 저장 실패", e);
        }
    }

    public void deleteRoomData(String roomId) {
        String roomKey = getRoomKey(roomId);
        redisTemplate.delete(roomKey);
    }

    public void deleteAllRelatedRoomsData(String roomId, Long memberId) {
        Set<String> roomKeys = new HashSet<>();
        roomKeys.add(String.format(ROOM_PLAYERS_KEY, roomId));
        roomKeys.add(String.format(SESSION_ROOM_KEY, roomId));

        if (roomKeys != null && !roomKeys.isEmpty()) {
            redisTemplate.delete(roomKeys);
        }
        removePlayerFromRoom(roomId, memberId);
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

    public boolean isPlayerInRoom(String roomId, Long memberId) {
        String roomKey = getRoomKey(roomId);
        return gameRoomRedisRepository.findPlayer(roomKey, memberId.toString()) != null;
    }

    public Optional<GameRoomPlayerInfo> getRoomPlayer(String roomId, Long memberId) {
        return Optional.ofNullable(findPlayer(roomId, memberId));
    }

    public ScreenStateUpdateResult updatePlayerScreenStateIfNewer(
            String roomId,
            Long memberId,
            MultiplayerScreenState state,
            long seq,
            long updatedAt
    ) {
        String roomKey = getRoomKey(roomId);
        GameRoomRedisRepository.ScreenStateUpdateResult updateResult =
                gameRoomRedisRepository.updatePlayerScreenStateIfNewer(
                        roomKey,
                        memberId.toString(),
                        state.name(),
                        seq,
                        updatedAt
                );

        ScreenStateUpdateStatus status = mapScreenStateUpdateStatus(updateResult);
        if (status == ScreenStateUpdateStatus.NOT_FOUND || status == ScreenStateUpdateStatus.STALE) {
            return ScreenStateUpdateResult.of(status);
        }

        GameRoomPlayerInfo playerInfo = findPlayer(roomId, memberId);
        return ScreenStateUpdateResult.of(status, playerInfo);
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

    private ScreenStateUpdateStatus mapScreenStateUpdateStatus(GameRoomRedisRepository.ScreenStateUpdateResult result) {
        if (result == null) {
            return ScreenStateUpdateStatus.NOT_FOUND;
        }

        return switch (result) {
            case UPDATED -> ScreenStateUpdateStatus.UPDATED;
            case NO_OP -> ScreenStateUpdateStatus.NO_OP;
            case STALE -> ScreenStateUpdateStatus.STALE;
            case NOT_FOUND -> ScreenStateUpdateStatus.NOT_FOUND;
        };
    }

    private GameRoomPlayerInfo findPlayer(String roomId, Long memberId) {
        try {
            String roomKey = getRoomKey(roomId);
            String playerJson = gameRoomRedisRepository.findPlayer(roomKey, memberId.toString());
            if (playerJson == null) {
                return null;
            }

            return objectMapper.readValue(playerJson, GameRoomPlayerInfo.class);
        } catch (Exception e) {
            log.error("Failed to deserialize player from Redis - RoomId: {}, MemberId: {}", roomId, memberId, e);
            return null;
        }
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

        // #region agent log
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("c:\\KoSpot-backend\\.cursor\\debug.log"),
                    (java.time.Instant.now().toEpochMilli() + "|cannotJoinRoom|check|{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"location\":\"GameRoomRedisService.java:207\",\"message\":\"방 정원 확인\",\"data\":{\"roomId\":\"" + roomId + "\",\"currentCount\":" + currentCount + ",\"maxPlayers\":" + maxPlayers + ",\"canJoin\":" + canJoin + "}}\n").getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {}
        // #endregion

        return !canJoin;
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
