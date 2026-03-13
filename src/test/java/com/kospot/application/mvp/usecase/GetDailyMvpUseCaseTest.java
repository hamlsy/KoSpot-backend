package com.kospot.application.mvp.usecase;

import com.kospot.gamerank.domain.vo.RankLevel;
import com.kospot.gamerank.domain.vo.RankTier;
import com.kospot.mvp.application.adaptor.DailyMvpAdaptor;
import com.kospot.mvp.application.service.DailyMvpReconcileService;
import com.kospot.mvp.domain.entity.DailyMvp;
import com.kospot.mvp.domain.vo.MvpCandidateSnapshot;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.mvp.infrastructure.redis.service.DailyMvpCandidateCacheService;
import com.kospot.mvp.infrastructure.redis.service.DailyMvpCacheService;
import com.kospot.mvp.application.usecase.GetDailyMvpUseCase;
import com.kospot.mvp.presentation.response.DailyMvpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetDailyMvpUseCaseTest {

    @Mock
    private DailyMvpAdaptor dailyMvpAdaptor;
    @Mock
    private DailyMvpCacheService dailyMvpCacheService;
    @Mock
    private DailyMvpCandidateCacheService dailyMvpCandidateCacheService;
    @Mock
    private DailyMvpReconcileService dailyMvpReconcileService;
    @Mock
    private MemberProfileRedisAdaptor memberProfileRedisAdaptor;

    private GetDailyMvpUseCase getDailyMvpUseCase;

    @BeforeEach
    void setUp() {
        getDailyMvpUseCase = new GetDailyMvpUseCase(
                dailyMvpAdaptor,
                dailyMvpCacheService,
                dailyMvpCandidateCacheService,
                dailyMvpReconcileService,
                memberProfileRedisAdaptor
        );
    }

    @Test
    @DisplayName("캐시 히트면 DB 조회 없이 반환한다")
    void shouldReturnCachedResponse() {
        LocalDate date = LocalDate.of(2026, 2, 28);
        DailyMvpResponse.Daily cached = DailyMvpResponse.Daily.builder()
                .mvpDate(date)
                .memberId(1L)
                .nickname("cached-user")
                .rankTier(RankTier.GOLD)
                .rankLevel(RankLevel.ONE)
                .ratingScore(2500)
                .gameScore(999)
                .poiName("홍대")
                .build();

        when(dailyMvpCacheService.get(date)).thenReturn(Optional.of(cached));

        DailyMvpResponse.Daily result = getDailyMvpUseCase.execute(date);

        assertNotNull(result);
        assertEquals("cached-user", result.getNickname());
        verifyNoInteractions(dailyMvpAdaptor, memberProfileRedisAdaptor);
    }

    @Test
    @DisplayName("캐시 미스 + DB 미존재면 none 캐시 저장 후 null 반환")
    void shouldCacheNoneWhenNoData() {
        LocalDate date = LocalDate.of(2026, 2, 27);
        when(dailyMvpCacheService.get(date)).thenReturn(Optional.empty());
        when(dailyMvpCacheService.isNoneCached(date)).thenReturn(false);
        when(dailyMvpCacheService.tryAcquireRebuildLock(date)).thenReturn(true);
        when(dailyMvpAdaptor.queryByDate(date)).thenReturn(Optional.empty());

        DailyMvpResponse.Daily result = getDailyMvpUseCase.execute(date);

        assertNull(result);
        verify(dailyMvpCacheService).cacheNone(date);
        verify(dailyMvpCacheService).releaseRebuildLock(date);
    }

    @Test
    @DisplayName("캐시 미스 + DB 존재면 프로필 결합 후 캐시 저장")
    void shouldLoadFromDbAndCache() {
        LocalDate date = LocalDate.of(2026, 2, 26);
        DailyMvp dailyMvp = DailyMvp.builder()
                .mvpDate(date)
                .memberId(2L)
                .roadViewGameId(77L)
                .poiName("종로")
                .gameScore(950.0)
                .rankTier(RankTier.PLATINUM)
                .rankLevel(RankLevel.THREE)
                .ratingScore(3100)
                .rewardPoint(200)
                .rewardGranted(false)
                .build();

        when(dailyMvpCacheService.get(date)).thenReturn(Optional.empty());
        when(dailyMvpCacheService.isNoneCached(date)).thenReturn(false);
        when(dailyMvpCacheService.tryAcquireRebuildLock(date)).thenReturn(true);
        when(dailyMvpAdaptor.queryByDate(date)).thenReturn(Optional.of(dailyMvp));
        when(memberProfileRedisAdaptor.findProfile(2L))
                .thenReturn(new MemberProfileRedisAdaptor.MemberProfileView(2L, "db-user", "marker-url"));

        DailyMvpResponse.Daily result = getDailyMvpUseCase.execute(date);

        assertNotNull(result);
        assertEquals("db-user", result.getNickname());
        verify(dailyMvpCacheService).cache(eq(date), any(DailyMvpResponse.Daily.class));
        verify(dailyMvpCacheService).releaseRebuildLock(date);
    }

    @Test
    @DisplayName("오늘 날짜에 DB 미존재여도 candidate가 있으면 응답하고 reconcile 호출")
    void shouldReturnCandidateForTodayWhenDbMissing() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        MvpCandidateSnapshot snapshot = new MvpCandidateSnapshot(
                3L,
                101L,
                "강남",
                975.5,
                LocalDateTime.now(),
                RankTier.DIAMOND,
                RankLevel.ONE,
                3550
        );

        when(dailyMvpCacheService.get(today)).thenReturn(Optional.empty());
        when(dailyMvpCacheService.tryAcquireRebuildLock(today)).thenReturn(true);
        when(dailyMvpAdaptor.queryByDate(today)).thenReturn(Optional.empty());
        when(dailyMvpCandidateCacheService.get(today)).thenReturn(Optional.of(snapshot));
        when(memberProfileRedisAdaptor.findProfile(3L))
                .thenReturn(new MemberProfileRedisAdaptor.MemberProfileView(3L, "candidate-user", "marker-url"));

        DailyMvpResponse.Daily result = getDailyMvpUseCase.execute(today);

        assertNotNull(result);
        assertEquals("candidate-user", result.getNickname());
        verify(dailyMvpCacheService).cache(eq(today), any(DailyMvpResponse.Daily.class));
        verify(dailyMvpReconcileService).reconcileByDateInNewTransaction(today);
        verify(dailyMvpCacheService, never()).cacheNone(today);
        verify(dailyMvpCacheService).releaseRebuildLock(today);
    }

    @Test
    @DisplayName("오늘 날짜에 none 캐시가 있어도 candidate가 있으면 즉시 반환한다")
    void shouldReturnCandidateWhenTodayNoneCacheExists() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        MvpCandidateSnapshot snapshot = new MvpCandidateSnapshot(
                4L,
                202L,
                "여의도",
                990.0,
                LocalDateTime.now(),
                RankTier.MASTER,
                RankLevel.ONE,
                3800
        );

        when(dailyMvpCacheService.get(today)).thenReturn(Optional.empty());
        when(dailyMvpCacheService.isNoneCached(today)).thenReturn(true);
        when(dailyMvpCandidateCacheService.get(today)).thenReturn(Optional.of(snapshot));
        when(memberProfileRedisAdaptor.findProfile(4L))
                .thenReturn(new MemberProfileRedisAdaptor.MemberProfileView(4L, "today-candidate", "marker-url"));

        DailyMvpResponse.Daily result = getDailyMvpUseCase.execute(today);

        assertNotNull(result);
        assertEquals("today-candidate", result.getNickname());
        verify(dailyMvpCacheService).cache(eq(today), any(DailyMvpResponse.Daily.class));
        verify(dailyMvpReconcileService).reconcileByDateInNewTransaction(today);
        verify(dailyMvpCacheService, never()).tryAcquireRebuildLock(today);
        verifyNoInteractions(dailyMvpAdaptor);
    }
}
