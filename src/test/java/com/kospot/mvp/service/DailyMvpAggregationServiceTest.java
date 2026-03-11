package com.kospot.mvp.service;

import com.kospot.game.application.adaptor.RoadViewGameAdaptor;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.domain.vo.GameMode;
import com.kospot.gamerank.application.adaptor.GameRankAdaptor;
import com.kospot.gamerank.domain.entity.GameRank;
import com.kospot.gamerank.domain.vo.RankLevel;
import com.kospot.gamerank.domain.vo.RankTier;
import com.kospot.member.domain.entity.Member;
import com.kospot.mvp.application.adaptor.DailyMvpAdaptor;
import com.kospot.mvp.domain.entity.DailyMvp;
import com.kospot.mvp.application.service.DailyMvpAggregationService;
import com.kospot.mvp.infrastructure.redis.service.DailyMvpCacheService;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyMvpAggregationServiceTest {

    @Mock
    private RoadViewGameAdaptor roadViewGameAdaptor;
    @Mock
    private GameRankAdaptor gameRankAdaptor;
    @Mock
    private DailyMvpAdaptor dailyMvpAdaptor;
    @Mock
    private DailyMvpCacheService dailyMvpCacheService;

    private DailyMvpAggregationService dailyMvpAggregationService;

    @BeforeEach
    void setUp() {
        dailyMvpAggregationService = new DailyMvpAggregationService(
                roadViewGameAdaptor,
                gameRankAdaptor,
                dailyMvpAdaptor,
                dailyMvpCacheService
        );
        ReflectionTestUtils.setField(dailyMvpAggregationService, "mvpRewardPoint", 200);
    }

    @Test
    @DisplayName("후보가 없으면 none 캐시를 저장한다")
    void aggregateByDateNoCandidate() {
        LocalDate date = LocalDate.of(2026, 2, 28);
        when(roadViewGameAdaptor.queryDailyMvpCandidate(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        Optional<DailyMvp> result = dailyMvpAggregationService.aggregateByDate(date);

        assertTrue(result.isEmpty());
        verify(dailyMvpCacheService).cacheNone(date);
        verifyNoInteractions(gameRankAdaptor, dailyMvpAdaptor);
    }

    @Test
    @DisplayName("후보가 있으면 일일 MVP를 생성하고 캐시를 무효화한다")
    void aggregateByDateCreateMvp() {
        LocalDate date = LocalDate.of(2026, 2, 28);
        Member member = Member.builder().id(1L).nickname("tester").build();
        RoadViewGame game = RoadViewGame.builder()
                .id(100L)
                .member(member)
                .poiName("강남역")
                .score(980.0)
                .build();
        GameRank gameRank = GameRank.builder()
                .member(member)
                .rankTier(RankTier.GOLD)
                .rankLevel(RankLevel.TWO)
                .ratingScore(2400)
                .build();
        DailyMvp saved = DailyMvp.builder()
                .id(11L)
                .mvpDate(date)
                .memberId(member.getId())
                .roadViewGameId(game.getId())
                .poiName(game.getPoiName())
                .gameScore(game.getScore())
                .rankTier(RankTier.GOLD)
                .rankLevel(RankLevel.TWO)
                .ratingScore(2400)
                .rewardPoint(200)
                .rewardGranted(false)
                .build();

        when(roadViewGameAdaptor.queryDailyMvpCandidate(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(game));
        when(gameRankAdaptor.queryByMemberAndGameMode(member, GameMode.ROADVIEW))
                .thenReturn(gameRank);
        when(dailyMvpAdaptor.queryByDate(date)).thenReturn(Optional.empty());
        when(dailyMvpAdaptor.save(any(DailyMvp.class))).thenReturn(saved);

        Optional<DailyMvp> result = dailyMvpAggregationService.aggregateByDate(date);

        assertTrue(result.isPresent());
        verify(dailyMvpAdaptor).save(any(DailyMvp.class));
        verify(dailyMvpCacheService).evict(date);
    }
}
