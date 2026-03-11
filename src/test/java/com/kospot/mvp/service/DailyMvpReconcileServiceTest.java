package com.kospot.mvp.service;

import com.kospot.game.application.adaptor.RoadViewGameAdaptor;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.gamerank.domain.vo.RankLevel;
import com.kospot.gamerank.domain.vo.RankTier;
import com.kospot.member.domain.entity.Member;
import com.kospot.mvp.application.adaptor.DailyMvpAdaptor;
import com.kospot.mvp.application.service.DailyMvpAggregationService;
import com.kospot.mvp.application.service.DailyMvpReconcileService;
import com.kospot.mvp.domain.entity.DailyMvp;
import com.kospot.mvp.domain.policy.MvpCandidateComparator;
import com.kospot.mvp.domain.vo.MvpCandidateSnapshot;
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
class DailyMvpReconcileServiceTest {

    @Mock
    private DailyMvpCandidateCacheService candidateCacheService;
    @Mock
    private DailyMvpAdaptor dailyMvpAdaptor;
    @Mock
    private DailyMvpCacheService dailyMvpCacheService;
    @Mock
    private DailyMvpAggregationService dailyMvpAggregationService;
    @Mock
    private RoadViewGameAdaptor roadViewGameAdaptor;

    private DailyMvpReconcileService service;

    @BeforeEach
    void setUp() {
        service = new DailyMvpReconcileService(
                candidateCacheService,
                dailyMvpAdaptor,
                dailyMvpCacheService,
                dailyMvpAggregationService,
                roadViewGameAdaptor,
                new MvpCandidateComparator()
        );
        ReflectionTestUtils.setField(service, "mvpRewardPoint", 200);
        ReflectionTestUtils.setField(service, "fullScanFallbackEnabled", false);
    }

    @Test
    @DisplayName("후보 캐시 미스 + fallback 비활성화면 no-op")
    void reconcileSkipsWhenCandidateMissingAndFallbackDisabled() {
        LocalDate date = LocalDate.of(2026, 3, 11);
        when(candidateCacheService.get(date)).thenReturn(Optional.empty());

        boolean changed = service.reconcileByDate(date);

        assertFalse(changed);
        verifyNoInteractions(dailyMvpAdaptor, dailyMvpAggregationService);
    }

    @Test
    @DisplayName("후보가 있고 DB row가 없으면 신규 DailyMvp를 생성한다")
    void reconcileCreatesWhenMissingRow() {
        LocalDate date = LocalDate.of(2026, 3, 11);
        MvpCandidateSnapshot candidate = new MvpCandidateSnapshot(
                1L,
                101L,
                "강남역",
                970.0,
                LocalDateTime.of(2026, 3, 11, 10, 0),
                RankTier.GOLD,
                RankLevel.ONE,
                2500
        );
        when(candidateCacheService.get(date)).thenReturn(Optional.of(candidate));
        when(dailyMvpAdaptor.queryByDateForUpdate(date)).thenReturn(Optional.empty());
        when(dailyMvpAdaptor.save(any(DailyMvp.class))).thenReturn(mock(DailyMvp.class));

        boolean changed = service.reconcileByDate(date);

        assertTrue(changed);
        verify(dailyMvpAdaptor).save(any(DailyMvp.class));
        verify(dailyMvpCacheService).evict(date);
    }

    @Test
    @DisplayName("DB가 더 우수하면 Redis 후보 캐시를 복구한다")
    void reconcileRepairsCandidateCacheWhenDbIsBetter() {
        LocalDate date = LocalDate.of(2026, 3, 11);
        MvpCandidateSnapshot redisCandidate = new MvpCandidateSnapshot(
                2L,
                201L,
                "서울역",
                910.0,
                LocalDateTime.of(2026, 3, 11, 10, 5),
                RankTier.SILVER,
                RankLevel.ONE,
                1500
        );
        DailyMvp current = DailyMvp.builder()
                .mvpDate(date)
                .memberId(1L)
                .roadViewGameId(101L)
                .poiName("강남역")
                .gameScore(980.0)
                .rankTier(RankTier.GOLD)
                .rankLevel(RankLevel.ONE)
                .ratingScore(2500)
                .rewardPoint(200)
                .rewardGranted(false)
                .build();
        RoadViewGame currentGame = RoadViewGame.builder()
                .id(101L)
                .member(Member.builder().id(1L).nickname("tester").build())
                .score(980.0)
                .poiName("강남역")
                .endedAt(LocalDateTime.of(2026, 3, 11, 9, 55))
                .build();

        when(candidateCacheService.get(date)).thenReturn(Optional.of(redisCandidate));
        when(dailyMvpAdaptor.queryByDateForUpdate(date)).thenReturn(Optional.of(current));
        when(roadViewGameAdaptor.queryById(101L)).thenReturn(currentGame);
        when(candidateCacheService.compareAndSetIfBetter(eq(date), any(MvpCandidateSnapshot.class))).thenReturn(true);

        boolean changed = service.reconcileByDate(date);

        assertFalse(changed);
        verify(candidateCacheService).compareAndSetIfBetter(eq(date), any(MvpCandidateSnapshot.class));
        verify(dailyMvpAdaptor, never()).save(any(DailyMvp.class));
    }
}
