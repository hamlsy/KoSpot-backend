package com.kospot.mvp.application.service;

import com.kospot.game.application.adaptor.RoadViewGameAdaptor;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.mvp.application.adaptor.DailyMvpAdaptor;
import com.kospot.mvp.domain.entity.DailyMvp;
import com.kospot.mvp.domain.policy.MvpCandidateComparator;
import com.kospot.mvp.domain.vo.MvpCandidateSnapshot;
import com.kospot.mvp.infrastructure.redis.service.DailyMvpCacheService;
import com.kospot.mvp.infrastructure.redis.service.DailyMvpCandidateCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyMvpReconcileService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DailyMvpCandidateCacheService dailyMvpCandidateCacheService;
    private final DailyMvpAdaptor dailyMvpAdaptor;
    private final DailyMvpCacheService dailyMvpCacheService;
    private final DailyMvpAggregationService dailyMvpAggregationService;
    private final RoadViewGameAdaptor roadViewGameAdaptor;
    private final MvpCandidateComparator comparator;

    @Value("${mvp.reward-point:200}")
    private int mvpRewardPoint;

    @Value("${mvp.aggregate-full-scan-fallback-enabled:false}")
    private boolean fullScanFallbackEnabled;

    @Transactional
    public int reconcileRecent() {
        LocalDate today = LocalDate.now(KST);
        List<LocalDate> dates = new ArrayList<>();
        dates.add(today.minusDays(1));
        dates.add(today);

        int fixedCount = 0;
        for (LocalDate date : dates) {
            if (reconcileByDate(date)) {
                fixedCount++;
            }
        }

        log.info("Daily MVP reconcile completed. fixedCount={}, dates={}", fixedCount, dates);
        return fixedCount;
    }

    @Transactional
    public boolean reconcileByDate(LocalDate targetDate) {
        Optional<MvpCandidateSnapshot> candidateOpt = dailyMvpCandidateCacheService.get(targetDate);
        if (candidateOpt.isEmpty()) {
            if (fullScanFallbackEnabled) {
                log.info("Candidate cache miss, fallback full-scan aggregation. date={}", targetDate);
                return dailyMvpAggregationService.aggregateByDate(targetDate).isPresent();
            }

            log.debug("Candidate cache miss and full-scan fallback disabled. date={}", targetDate);
            return false;
        }

        MvpCandidateSnapshot candidateSnapshot = candidateOpt.get();
        DailyMvp current = dailyMvpAdaptor.queryByDateForUpdate(targetDate).orElse(null);

        if (current == null) {
            dailyMvpAdaptor.save(DailyMvp.create(targetDate, candidateSnapshot, mvpRewardPoint));
            dailyMvpCacheService.evict(targetDate);
            log.info("Reconcile created missing DailyMvp row. date={}, gameId={}", targetDate, candidateSnapshot.roadViewGameId());
            return true;
        }

        if (current.getRoadViewGameId().equals(candidateSnapshot.roadViewGameId())) {
            return false;
        }

        MvpCandidateSnapshot currentSnapshot = toSnapshot(current);
        if (comparator.isBetter(candidateSnapshot, currentSnapshot)) {
            current.updateSnapshot(candidateSnapshot);
            dailyMvpAdaptor.save(current);
            dailyMvpCacheService.evict(targetDate);
            log.info(
                    "Reconcile updated DailyMvp from candidate cache. date={}, fromGameId={}, toGameId={}",
                    targetDate,
                    currentSnapshot.roadViewGameId(),
                    candidateSnapshot.roadViewGameId()
            );
            return true;
        }

        boolean repaired = dailyMvpCandidateCacheService.compareAndSetIfBetter(targetDate, currentSnapshot);
        if (repaired) {
            log.info("Reconcile repaired candidate cache from DB snapshot. date={}, gameId={}", targetDate, currentSnapshot.roadViewGameId());
        }
        return false;
    }

    private MvpCandidateSnapshot toSnapshot(DailyMvp current) {
        RoadViewGame currentGame = roadViewGameAdaptor.queryById(current.getRoadViewGameId());
        return MvpCandidateSnapshot.from(current, currentGame.getEndedAt());
    }
}
