package com.kospot.mvp.application.service;

import com.kospot.game.application.adaptor.RoadViewGameAdaptor;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.gamerank.domain.entity.GameRank;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyMvpIncrementalAggregationService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DailyMvpCandidateCacheService dailyMvpCandidateCacheService;
    private final DailyMvpAdaptor dailyMvpAdaptor;
    private final DailyMvpCacheService dailyMvpCacheService;
    private final MvpCandidateComparator comparator;
    private final RoadViewGameAdaptor roadViewGameAdaptor;

    @Value("${mvp.reward-point:200}")
    private int mvpRewardPoint;

    @Transactional
    public boolean aggregate(RoadViewGame game, GameRank gameRank) {
        LocalDate mvpDate = game.getEndedAt().atZone(KST).toLocalDate();
        MvpCandidateSnapshot candidateSnapshot = MvpCandidateSnapshot.from(game, gameRank);

        boolean replaced = dailyMvpCandidateCacheService.compareAndSetIfBetter(mvpDate, candidateSnapshot);
        if (!replaced) {
            log.debug("Skip MVP snapshot update. mvpDate={}, gameId={}", mvpDate, game.getId());
            return false;
        }

        boolean updated = upsertDailyMvp(mvpDate, game, gameRank, candidateSnapshot);
        if (updated) {
            dailyMvpCacheService.evict(mvpDate);
        }
        return updated;
    }

    private boolean upsertDailyMvp(
            LocalDate mvpDate,
            RoadViewGame candidateGame,
            GameRank gameRank,
            MvpCandidateSnapshot candidateSnapshot
    ) {
        DailyMvp locked = dailyMvpAdaptor.queryByDateForUpdate(mvpDate).orElse(null);
        if (locked == null) {
            dailyMvpAdaptor.save(DailyMvp.create(mvpDate, candidateGame, gameRank, mvpRewardPoint));
            return true;
        }

        if (locked.getRoadViewGameId().equals(candidateGame.getId())) {
            return false;
        }

        RoadViewGame currentGame = roadViewGameAdaptor.queryById(locked.getRoadViewGameId());
        MvpCandidateSnapshot currentSnapshot = MvpCandidateSnapshot.from(locked, currentGame.getEndedAt());
        if (!comparator.isBetter(candidateSnapshot, currentSnapshot)) {
            return false;
        }

        locked.updateSnapshot(candidateGame, gameRank);
        dailyMvpAdaptor.save(locked);
        return true;
    }
}
