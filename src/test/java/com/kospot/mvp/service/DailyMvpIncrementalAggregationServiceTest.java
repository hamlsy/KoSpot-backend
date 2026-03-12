package com.kospot.mvp.service;

import com.kospot.game.application.adaptor.RoadViewGameAdaptor;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.gamerank.domain.entity.GameRank;
import com.kospot.gamerank.domain.vo.RankLevel;
import com.kospot.gamerank.domain.vo.RankTier;
import com.kospot.member.domain.entity.Member;
import com.kospot.mvp.application.adaptor.DailyMvpAdaptor;
import com.kospot.mvp.application.service.DailyMvpIncrementalAggregationService;
import com.kospot.mvp.domain.entity.DailyMvp;
import com.kospot.mvp.domain.policy.MvpCandidateComparator;
import com.kospot.mvp.infrastructure.redis.service.DailyMvpCacheService;
import com.kospot.mvp.infrastructure.redis.service.DailyMvpCandidateCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyMvpIncrementalAggregationServiceTest {

    @Mock
    private DailyMvpCandidateCacheService candidateCacheService;
    @Mock
    private DailyMvpAdaptor dailyMvpAdaptor;
    @Mock
    private DailyMvpCacheService dailyMvpCacheService;
    @Mock
    private RoadViewGameAdaptor roadViewGameAdaptor;

    private DailyMvpIncrementalAggregationService service;

    @BeforeEach
    void setUp() {
        service = new DailyMvpIncrementalAggregationService(
                candidateCacheService,
                dailyMvpAdaptor,
                dailyMvpCacheService,
                new MvpCandidateComparator(),
                roadViewGameAdaptor
        );
        ReflectionTestUtils.setField(service, "mvpRewardPoint", 200);
    }

    @Test
    @DisplayName("Redis 후보 교체 실패 시 DB 반영하지 않는다")
    void aggregateSkipsWhenCandidateNotReplaced() {
        RoadViewGame game = game(10L, 980.0, LocalDateTime.of(2026, 3, 11, 9, 0));
        GameRank rank = rank(game.getMember());
        when(candidateCacheService.compareAndSetIfBetter(any(LocalDate.class), any())).thenReturn(false);

        boolean result = service.aggregate(game, rank);

        assertFalse(result);
        verifyNoInteractions(dailyMvpAdaptor, dailyMvpCacheService);
    }

    @Test
    @DisplayName("Redis 후보 교체 성공 + 기존 row 없음이면 신규 저장 후 캐시 무효화한다")
    void aggregateCreatesSnapshotWhenNoDailyMvp() {
        RoadViewGame game = game(10L, 980.0, LocalDateTime.of(2026, 3, 11, 9, 0));
        GameRank rank = rank(game.getMember());
        when(candidateCacheService.compareAndSetIfBetter(any(LocalDate.class), any())).thenReturn(true);
        when(dailyMvpAdaptor.queryByDateForUpdate(LocalDate.of(2026, 3, 11))).thenReturn(Optional.empty());
        when(dailyMvpAdaptor.save(any(DailyMvp.class))).thenReturn(mock(DailyMvp.class));

        boolean result = service.aggregate(game, rank);

        assertTrue(result);
        verify(dailyMvpAdaptor).save(any(DailyMvp.class));
        verify(dailyMvpCacheService).evict(LocalDate.of(2026, 3, 11));
    }

    private RoadViewGame game(Long gameId, double score, LocalDateTime endedAt) {
        Member member = Member.builder().id(1L).nickname("tester").build();
        return RoadViewGame.builder()
                .id(gameId)
                .member(member)
                .score(score)
                .poiName("강남역")
                .endedAt(endedAt)
                .build();
    }

    private GameRank rank(Member member) {
        return GameRank.builder()
                .member(member)
                .rankTier(RankTier.GOLD)
                .rankLevel(RankLevel.TWO)
                .ratingScore(2400)
                .build();
    }
}
