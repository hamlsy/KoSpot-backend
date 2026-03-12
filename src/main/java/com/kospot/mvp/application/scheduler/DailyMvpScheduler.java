package com.kospot.mvp.application.scheduler;

import com.kospot.mvp.application.service.DailyMvpAggregationService;
import com.kospot.mvp.application.service.DailyMvpReconcileService;
import com.kospot.mvp.application.service.DailyMvpRewardService;
import com.kospot.mvp.infrastructure.redis.service.DailyMvpCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyMvpScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String AGGREGATION_JOB = "aggregation";
    private static final String REWARD_JOB = "reward";

    private final DailyMvpAggregationService dailyMvpAggregationService;
    private final DailyMvpReconcileService dailyMvpReconcileService;
    private final DailyMvpRewardService dailyMvpRewardService;
    private final DailyMvpCacheService dailyMvpCacheService;

    @Value("${mvp.reconcile-only.enabled:true}")
    private boolean reconcileOnlyEnabled;

    @Scheduled(cron = "${mvp.aggregate-cron:0 0 * * * *}")
    public void aggregateHourly() {
        if (!dailyMvpCacheService.tryAcquireScheduleLock(AGGREGATION_JOB, Duration.ofMinutes(55))) {
            return;
        }

        try {
            if (reconcileOnlyEnabled) {
                dailyMvpReconcileService.reconcileRecent();
            } else {
                dailyMvpAggregationService.aggregateToday();
            }
        } catch (Exception e) {
            log.error("Failed to aggregate daily MVP", e);
        } finally {
            dailyMvpCacheService.releaseScheduleLock(AGGREGATION_JOB);
        }
    }

    @Scheduled(cron = "${mvp.reward-cron:0 5 0 * * *}")
    public void rewardPreviousDay() {
        if (!dailyMvpCacheService.tryAcquireScheduleLock(REWARD_JOB, Duration.ofMinutes(10))) {
            return;
        }

        try {
            LocalDate yesterday = LocalDate.now(KST).minusDays(1);
            dailyMvpRewardService.rewardUnprocessedUpTo(yesterday);
        } catch (Exception e) {
            log.error("Failed to reward daily MVP", e);
        } finally {
            dailyMvpCacheService.releaseScheduleLock(REWARD_JOB);
        }
    }
}
