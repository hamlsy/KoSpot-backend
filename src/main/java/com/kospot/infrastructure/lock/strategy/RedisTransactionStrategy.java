package com.kospot.infrastructure.lock.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.lock.vo.HostAssignmentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Redis Transaction (WATCH/MULTI/EXEC)을 사용한 동시성 제어 전략
 * 
 * 특징:
 * - 낙관적 락(Optimistic Lock)
 * - 충돌 시 재시도 필요
 * - 추가 라이브러리 불필요
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTransactionStrategy implements HostAssignmentLockStrategy {

    private static final String ROOM_PLAYERS_KEY = "game:room:%s:players";
    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MS = 50;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public HostAssignmentResult executeWithLock(
            String roomId,
            Long leavingMemberId,
            Supplier<HostAssignmentResult> operation) {

        String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);

        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                HostAssignmentResult result = executeTransaction(roomKey, leavingMemberId);
                if (result != null && result.isSuccess()) {
                    log.debug("Transaction succeeded on attempt {} - RoomId: {}", attempt, roomId);
                    return result;
                }
            } catch (Exception e) {
                log.warn("Transaction failed on attempt {} - RoomId: {}, Error: {}",
                        attempt, roomId, e.getMessage());
            }

            if (attempt < MAX_RETRY_COUNT) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return HostAssignmentResult.failure("재시도 중 인터럽트 발생");
                }
            }
        }

        log.error("All transaction attempts failed - RoomId: {}, MemberId: {}", roomId, leavingMemberId);
        return HostAssignmentResult.failure("최대 재시도 횟수 초과");
    }

    @SuppressWarnings("unchecked")
    private HostAssignmentResult executeTransaction(String roomKey, Long leavingMemberId) {
        return redisTemplate.execute(new SessionCallback<HostAssignmentResult>() {
            @Override
            public HostAssignmentResult execute(RedisOperations operations) throws DataAccessException {
                // 1. WATCH: 키 변경 감시 시작
                operations.watch(roomKey);

                // 2. 현재 플레이어 목록 조회
                Map<Object, Object> playersMap = operations.opsForHash().entries(roomKey);
                if (playersMap.isEmpty()) {
                    operations.unwatch();
                    return HostAssignmentResult.failure("방에 플레이어가 없음");
                }

                List<GameRoomPlayerInfo> players = parsePlayersFromMap(playersMap);

                // 퇴장 플레이어 찾기
                GameRoomPlayerInfo leavingPlayer = players.stream()
                        .filter(p -> p.getMemberId().equals(leavingMemberId))
                        .findFirst()
                        .orElse(null);

                if (leavingPlayer == null) {
                    operations.unwatch();
                    return HostAssignmentResult.failure("퇴장 플레이어를 찾을 수 없음");
                }

                // 3. 방장 재지정 로직
                boolean isHost = leavingPlayer.isHost();
                List<GameRoomPlayerInfo> remainingPlayers = players.stream()
                        .filter(p -> !p.getMemberId().equals(leavingMemberId))
                        .collect(Collectors.toList());

                // 4. MULTI: 트랜잭션 시작
                operations.multi();

                // 5. 플레이어 제거
                operations.opsForHash().delete(roomKey, leavingMemberId.toString());

                HostAssignmentResult.Action action;
                GameRoomPlayerInfo newHost = null;

                if (remainingPlayers.isEmpty()) {
                    // 마지막 플레이어 - 방 삭제
                    operations.delete(roomKey);
                    action = HostAssignmentResult.Action.DELETE_ROOM;
                } else if (isHost) {
                    // 방장 퇴장 - 다음 방장 선정
                    Long leavingJoinedAt = leavingPlayer.getJoinedAt();
                    newHost = remainingPlayers.stream()
                            .filter(p -> p.getJoinedAt() != null && p.getJoinedAt() > leavingJoinedAt)
                            .min(Comparator.comparing(GameRoomPlayerInfo::getJoinedAt))
                            .orElse(remainingPlayers.get(0));

                    newHost.setHost(true);
                    try {
                        String newHostJson = objectMapper.writeValueAsString(newHost);
                        operations.opsForHash().put(roomKey, newHost.getMemberId().toString(), newHostJson);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("JSON 직렬화 실패", e);
                    }
                    action = HostAssignmentResult.Action.CHANGE_HOST;
                } else {
                    // 일반 플레이어 퇴장
                    action = HostAssignmentResult.Action.NORMAL_LEAVE;
                }

                // 6. EXEC: 트랜잭션 실행
                List<Object> execResult = operations.exec();

                if (execResult == null || execResult.isEmpty()) {
                    // WATCH 충돌 - 다른 클라이언트가 수정함
                    log.debug("Transaction aborted due to WATCH conflict - RoomKey: {}", roomKey);
                    return null;
                }

                return switch (action) {
                    case DELETE_ROOM -> HostAssignmentResult.deleteRoom(leavingMemberId, leavingPlayer);
                    case CHANGE_HOST -> HostAssignmentResult.changeHost(leavingMemberId, leavingPlayer, newHost);
                    case NORMAL_LEAVE -> HostAssignmentResult.normalLeave(leavingMemberId, leavingPlayer);
                };
            }
        });
    }

    private List<GameRoomPlayerInfo> parsePlayersFromMap(Map<Object, Object> playersMap) {
        return playersMap.values().stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue((String) json, GameRoomPlayerInfo.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to parse player info", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return "REDIS_TRANSACTION";
    }
}
