package com.kospot.mvp.service;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.mvp.adaptor.DailyMvpAdaptor;
import com.kospot.domain.mvp.entity.DailyMvp;
import com.kospot.domain.mvp.service.DailyMvpRewardService;
import com.kospot.domain.point.service.PointHistoryService;
import com.kospot.domain.point.service.PointService;
import com.kospot.domain.point.vo.PointHistoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyMvpRewardServiceTest {

    @Mock
    private DailyMvpAdaptor dailyMvpAdaptor;
    @Mock
    private MemberAdaptor memberAdaptor;
    @Mock
    private PointService pointService;
    @Mock
    private PointHistoryService pointHistoryService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DailyMvpRewardService dailyMvpRewardService;

    @BeforeEach
    void setUp() {
        dailyMvpRewardService = new DailyMvpRewardService(
                dailyMvpAdaptor,
                memberAdaptor,
                pointService,
                pointHistoryService,
                eventPublisher
        );
    }

    @Test
    @DisplayName("같은 대상에 대해 보상은 한 번만 처리된다")
    void rewardShouldBeIdempotent() {
        LocalDate targetDate = LocalDate.of(2026, 2, 28);
        DailyMvp dailyMvp = DailyMvp.builder()
                .mvpDate(targetDate)
                .memberId(10L)
                .roadViewGameId(100L)
                .rewardPoint(200)
                .rewardGranted(false)
                .rankLevel(com.kospot.domain.gamerank.vo.RankLevel.THREE)
                .rankTier(com.kospot.domain.gamerank.vo.RankTier.SILVER)
                .poiName("서울역")
                .gameScore(901.5)
                .ratingScore(1200)
                .build();
        Member member = Member.builder().id(10L).nickname("mvp-user").build();

        when(dailyMvpAdaptor.queryUnrewardedByDateLessThanEqual(targetDate)).thenReturn(List.of(dailyMvp));
        when(dailyMvpAdaptor.queryByDateForUpdate(targetDate)).thenReturn(Optional.of(dailyMvp));
        when(memberAdaptor.queryById(10L)).thenReturn(member);

        int first = dailyMvpRewardService.rewardUnprocessedUpTo(targetDate);
        int second = dailyMvpRewardService.rewardUnprocessedUpTo(targetDate);

        assertEquals(1, first);
        assertEquals(0, second);
        verify(pointService, times(1)).addPoint(member, 200);
        verify(pointHistoryService, times(1)).savePointHistory(member, 200, PointHistoryType.MVP_REWARD);
        verify(eventPublisher, times(1)).publishEvent(any());
    }
}
