package com.kospot.mvp.application.service;

import com.kospot.domain.game.adaptor.RoadViewGameAdaptor;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.gamerank.application.adaptor.GameRankAdaptor;
import com.kospot.gamerank.domain.entity.GameRank;
import com.kospot.mvp.application.adaptor.DailyMvpAdaptor;
import com.kospot.mvp.domain.entity.DailyMvp;
import com.kospot.infrastructure.redis.domain.mvp.service.DailyMvpCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyMvpAggregationService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RoadViewGameAdaptor roadViewGameAdaptor;
    private final GameRankAdaptor gameRankAdaptor;
    private final DailyMvpAdaptor dailyMvpAdaptor;
    private final DailyMvpCacheService dailyMvpCacheService;

    @Value("${mvp.reward-point:200}")
    private int mvpRewardPoint;

    @Transactional
    public Optional<DailyMvp> aggregateToday() {
        return aggregateByDate(LocalDate.now(KST));
    }

    @Transactional
    public Optional<DailyMvp> aggregateByDate(LocalDate targetDate) {
        LocalDateTime startAt = targetDate.atStartOfDay();
        LocalDateTime endAt = targetDate.plusDays(1).atStartOfDay();

        Optional<RoadViewGame> candidateOpt = roadViewGameAdaptor.queryDailyMvpCandidate(startAt, endAt);
        if (candidateOpt.isEmpty()) {
            dailyMvpCacheService.cacheNone(targetDate);
            log.info("No daily MVP candidate found. date={}", targetDate);
            return Optional.empty();
        }

        RoadViewGame candidate = candidateOpt.get();
        GameRank gameRank = gameRankAdaptor.queryByMemberAndGameMode(candidate.getMember(), GameMode.ROADVIEW);

        DailyMvp saved = dailyMvpAdaptor.queryByDate(targetDate)
                .map(existing -> {
                    existing.updateSnapshot(candidate, gameRank);
                    return dailyMvpAdaptor.save(existing);
                })
                .orElseGet(() -> dailyMvpAdaptor.save(DailyMvp.create(targetDate, candidate, gameRank, mvpRewardPoint)));

        dailyMvpCacheService.evict(targetDate);
        return Optional.of(saved);
    }
}
