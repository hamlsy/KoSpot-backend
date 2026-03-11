package com.kospot.mvp.application.service;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.mvp.application.adaptor.DailyMvpAdaptor;
import com.kospot.mvp.domain.entity.DailyMvp;
import com.kospot.mvp.domain.event.MvpRewardGrantedEvent;
import com.kospot.mvp.infrastructure.redis.service.DailyMvpCacheService;
import com.kospot.point.application.service.PointHistoryService;
import com.kospot.point.application.service.PointService;
import com.kospot.point.domain.vo.PointHistoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyMvpRewardService {

    private final DailyMvpAdaptor dailyMvpAdaptor;
    private final MemberAdaptor memberAdaptor;
    private final PointService pointService;
    private final PointHistoryService pointHistoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final DailyMvpCacheService dailyMvpCacheService;

    @Transactional
    public int rewardUnprocessedUpTo(LocalDate targetDate) {
        List<DailyMvp> unrewarded = dailyMvpAdaptor.queryUnrewardedByDateLessThanEqual(targetDate);
        int rewardedCount = 0;

        for (DailyMvp item : unrewarded) {
            DailyMvp locked = dailyMvpAdaptor.queryByDateForUpdate(item.getMvpDate())
                    .orElse(null);

            if (locked == null || locked.isRewardGranted()) {
                continue;
            }

            LocalDate mvpDate = locked.getMvpDate();
            Long memberId = locked.getMemberId();
            boolean acquired = dailyMvpCacheService.tryAcquireRewardLock(mvpDate, memberId, Duration.ofMinutes(2));
            if (!acquired) {
                continue;
            }

            if (dailyMvpCacheService.isRewardProcessed(mvpDate, memberId)) {
                dailyMvpCacheService.releaseRewardLock(mvpDate, memberId);
                log.info("Skip already processed MVP reward. mvpDate={}, memberId={}", mvpDate, memberId);
                continue;
            }

            try {
                Member member = memberAdaptor.queryById(memberId);
                pointService.addPoint(member, locked.getRewardPoint());
                pointHistoryService.savePointHistory(member, locked.getRewardPoint(), PointHistoryType.MVP_REWARD);

                locked.grantReward(LocalDateTime.now());
                rewardedCount++;

                eventPublisher.publishEvent(new MvpRewardGrantedEvent(
                        memberId,
                        mvpDate,
                        locked.getRoadViewGameId(),
                        locked.getRewardPoint()
                ));

                registerAfterCompletion(mvpDate, memberId);
            } catch (RuntimeException e) {
                dailyMvpCacheService.releaseRewardLock(mvpDate, memberId);
                throw e;
            }
        }

        log.info("Daily MVP rewards processed. targetDate={}, rewardedCount={}", targetDate, rewardedCount);
        return rewardedCount;
    }

    private void registerAfterCompletion(LocalDate mvpDate, Long memberId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            dailyMvpCacheService.markRewardProcessed(mvpDate, memberId);
            dailyMvpCacheService.releaseRewardLock(mvpDate, memberId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dailyMvpCacheService.markRewardProcessed(mvpDate, memberId);
            }

            @Override
            public void afterCompletion(int status) {
                dailyMvpCacheService.releaseRewardLock(mvpDate, memberId);
            }
        });
    }
}
