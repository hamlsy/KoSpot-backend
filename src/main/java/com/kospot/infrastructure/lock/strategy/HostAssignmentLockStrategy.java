package com.kospot.infrastructure.lock.strategy;

import com.kospot.infrastructure.lock.vo.HostAssignmentResult;

import java.util.function.Supplier;

/**
 * 게임방 방장 재지정 시 동시성 제어를 위한 전략 인터페이스
 * 
 * 구현체:
 * - RedissonLockStrategy: Redisson 분산 락
 * - RedisTransactionStrategy: Redis WATCH/MULTI/EXEC
 * - LuaScriptStrategy: Redis Lua Script
 */
public interface HostAssignmentLockStrategy {

    /**
     * 락을 획득하고 방장 재지정 작업을 원자적으로 수행
     *
     * @param roomId          게임방 ID
     * @param leavingMemberId 퇴장하는 멤버 ID
     * @param operation       실행할 작업
     * @return 방장 재지정 결과
     */
    HostAssignmentResult executeWithLock(
            String roomId,
            Long leavingMemberId,
            Supplier<HostAssignmentResult> operation);

    /**
     * 전략 이름 반환 (로깅/메트릭용)
     */
    String getStrategyName();
}
