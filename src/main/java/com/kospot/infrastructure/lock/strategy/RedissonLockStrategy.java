package com.kospot.infrastructure.lock.strategy;

import com.kospot.infrastructure.lock.vo.HostAssignmentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 분산 락을 사용한 동시성 제어 전략
 * 
 * 특징:
 * - 비관적 락(Pessimistic Lock)
 * - 자동 해제(Lease Time)
 * - 재진입 가능(Reentrant)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockStrategy implements HostAssignmentLockStrategy {

    private static final String LOCK_KEY_PREFIX = "lock:game:room:";
    private static final long WAIT_TIME = 5;
    private static final long LEASE_TIME = 10;

    private final RedissonClient redissonClient;

    @Override
    public HostAssignmentResult executeWithLock(
            String roomId,
            Long leavingMemberId,
            Supplier<HostAssignmentResult> operation) {

        String lockKey = LOCK_KEY_PREFIX + roomId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("Failed to acquire Redisson lock - RoomId: {}, MemberId: {}",
                        roomId, leavingMemberId);
                return HostAssignmentResult.failure("락 획득 실패: 작업 진행 중");
            }

            log.debug("Redisson lock acquired - RoomId: {}, MemberId: {}", roomId, leavingMemberId);
            return operation.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock acquisition interrupted - RoomId: {}", roomId, e);
            return HostAssignmentResult.failure("락 획득 중 인터럽트 발생");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Redisson lock released - RoomId: {}", roomId);
            }
        }
    }

    @Override
    public String getStrategyName() {
        return "REDISSON_LOCK";
    }
}
