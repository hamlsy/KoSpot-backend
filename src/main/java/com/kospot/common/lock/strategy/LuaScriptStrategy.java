package com.kospot.common.lock.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import com.kospot.multi.room.domain.vo.MultiplayerScreenState;
import com.kospot.common.lock.vo.HostAssignmentResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.function.Supplier;

/**
 * Redis Lua Script를 사용한 동시성 제어 전략
 * 
 * 특징:
 * - 완벽한 원자성 보장
 * - 네트워크 왕복 1회
 * - 스크립트 실행 중 다른 명령 블로킹
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LuaScriptStrategy implements HostAssignmentLockStrategy {

    private static final String ROOM_PLAYERS_KEY = "game:room:%s:players";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private DefaultRedisScript<String> leaveRoomScript;

    @PostConstruct
    public void init() {
        leaveRoomScript = new DefaultRedisScript<>();
        leaveRoomScript.setScriptText(getLuaScript());
        leaveRoomScript.setResultType(String.class);
    }

    @Override
    public HostAssignmentResult executeWithLock(
            String roomId,
            Long leavingMemberId,
            Supplier<HostAssignmentResult> operation) {

        String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);

        try {
            String resultJson = redisTemplate.execute(
                    leaveRoomScript,
                    Collections.singletonList(roomKey),
                    leavingMemberId.toString());

            if (resultJson == null || resultJson.isEmpty()) {
                return HostAssignmentResult.failureRetryable("Lua script returned empty result");
            }

            return parseLuaResult(resultJson, leavingMemberId);

        } catch (Exception e) {
            log.error("Lua script execution failed - RoomId: {}, MemberId: {}",
                    roomId, leavingMemberId, e);
            return HostAssignmentResult.failureRetryable("Lua script execution error: " + e.getMessage());
        }
    }

    private String getLuaScript() {
        return """
                -- KEYS[1]: room players hash key
                -- ARGV[1]: leaving member ID

                local roomKey = KEYS[1]
                local leavingMemberId = ARGV[1]

                -- 1. 퇴장 플레이어 정보 조회
                local leavingPlayerJson = redis.call('HGET', roomKey, leavingMemberId)
                if not leavingPlayerJson then
                    return cjson.encode({action = 'ERROR', message = 'Player not found'})
                end

                local leavingPlayer = cjson.decode(leavingPlayerJson)
                local isHost = leavingPlayer.isHost or leavingPlayer.host
                local leavingJoinedAt = leavingPlayer.joinedAt or 0

                -- 2. 플레이어 제거
                redis.call('HDEL', roomKey, leavingMemberId)

                -- 3. 남은 플레이어 확인
                local remainingPlayers = redis.call('HGETALL', roomKey)
                local playerCount = #remainingPlayers / 2

                if playerCount == 0 then
                    -- 마지막 플레이어 퇴장 - 방 삭제
                    redis.call('DEL', roomKey)
                    return cjson.encode({
                        action = 'DELETE_ROOM',
                        leavingMemberId = leavingMemberId,
                        leavingPlayer = leavingPlayer,
                        success = true
                    })
                end

                if not isHost then
                    -- 일반 플레이어 퇴장
                    return cjson.encode({
                        action = 'NORMAL_LEAVE',
                        leavingMemberId = leavingMemberId,
                        leavingPlayer = leavingPlayer,
                        success = true
                    })
                end

                -- 4. 방장 퇴장 - 다음 방장 선정
                local candidates = {}
                for i = 1, #remainingPlayers, 2 do
                    local memberId = remainingPlayers[i]
                    local playerJson = remainingPlayers[i + 1]
                    local player = cjson.decode(playerJson)
                    local joinedAt = player.joinedAt or 0

                    if joinedAt > leavingJoinedAt then
                        table.insert(candidates, {memberId = memberId, player = player, joinedAt = joinedAt})
                    end
                end

                local newHost = nil
                local newHostMemberId = nil

                if #candidates > 0 then
                    -- joinedAt이 가장 작은(가장 먼저 들어온) 후보 선정
                    table.sort(candidates, function(a, b) return a.joinedAt < b.joinedAt end)
                    newHostMemberId = candidates[1].memberId
                    newHost = candidates[1].player
                else
                    -- 후보가 없으면 첫 번째 남은 플레이어
                    newHostMemberId = remainingPlayers[1]
                    newHost = cjson.decode(remainingPlayers[2])
                end

                -- 5. 새 방장으로 업데이트
                newHost.isHost = true
                newHost.host = true
                redis.call('HSET', roomKey, newHostMemberId, cjson.encode(newHost))

                return cjson.encode({
                    action = 'CHANGE_HOST',
                    leavingMemberId = leavingMemberId,
                    leavingPlayer = leavingPlayer,
                    newHost = newHost,
                    newHostMemberId = newHostMemberId,
                    success = true
                })
                """;
    }

    private HostAssignmentResult parseLuaResult(String resultJson, Long leavingMemberId) {
        try {
            var resultNode = objectMapper.readTree(resultJson);
            String action = resultNode.get("action").asText();

            if ("ERROR".equals(action)) {
                String message = resultNode.has("message") ? resultNode.get("message").asText() : "Unknown error";
                if ("Player not found".equalsIgnoreCase(message)) {
                    return HostAssignmentResult.alreadyLeft(leavingMemberId);
                }
                return HostAssignmentResult.failureRetryable(message);
            }

            GameRoomPlayerInfo leavingPlayer = parsePlayerFromNode(resultNode.get("leavingPlayer"));

            return switch (action) {
                case "DELETE_ROOM" -> HostAssignmentResult.deleteRoom(leavingMemberId, leavingPlayer);
                case "NORMAL_LEAVE" -> HostAssignmentResult.normalLeave(leavingMemberId, leavingPlayer);
                case "CHANGE_HOST" -> {
                    GameRoomPlayerInfo newHost = parsePlayerFromNode(resultNode.get("newHost"));
                    yield HostAssignmentResult.changeHost(leavingMemberId, leavingPlayer, newHost);
                }
                default -> HostAssignmentResult.failureFatal("Unknown Lua action: " + action);
            };

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Lua script result - Result: {}", resultJson, e);
            return HostAssignmentResult.failureFatal("Failed to parse Lua result");
        }
    }

    private GameRoomPlayerInfo parsePlayerFromNode(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        return GameRoomPlayerInfo.builder()
                .memberId(node.has("memberId") ? node.get("memberId").asLong() : null)
                .nickname(node.has("nickname") ? node.get("nickname").asText() : null)
                .markerImageUrl(node.has("markerImageUrl") && !node.get("markerImageUrl").isNull()
                        ? node.get("markerImageUrl").asText()
                        : null)
                .isHost(node.has("isHost") ? node.get("isHost").asBoolean()
                        : (node.has("host") ? node.get("host").asBoolean() : false))
                .joinedAt(node.has("joinedAt") ? node.get("joinedAt").asLong() : null)
                .team(node.has("team") && !node.get("team").isNull() ? node.get("team").asText() : null)
                .screenState(parseScreenState(node))
                .screenStateSeq(node.has("screenStateSeq") ? node.get("screenStateSeq").asLong() : null)
                .screenStateUpdatedAt(node.has("screenStateUpdatedAt") ? node.get("screenStateUpdatedAt").asLong() : null)
                .build();
    }

    private MultiplayerScreenState parseScreenState(com.fasterxml.jackson.databind.JsonNode node) {
        if (!node.has("screenState") || node.get("screenState").isNull()) {
            return null;
        }

        try {
            return MultiplayerScreenState.valueOf(node.get("screenState").asText());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown screen state from lua result: {}", node.get("screenState").asText());
            return null;
        }
    }

    @Override
    public String getStrategyName() {
        return "LUA_SCRIPT";
    }
}
