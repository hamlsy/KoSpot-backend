package com.kospot.application.mvp.usecase;

import com.kospot.mvp.application.usecase.GetDailyMvpUseCase;
import com.kospot.mvp.application.usecase.GetTodayAndYesterdayMvpUseCase;
import com.kospot.mvp.presentation.dto.response.DailyMvpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTodayAndYesterdayMvpUseCaseTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private GetDailyMvpUseCase getDailyMvpUseCase;

    @InjectMocks
    private GetTodayAndYesterdayMvpUseCase getTodayAndYesterdayMvpUseCase;

    @Test
    @DisplayName("오늘과 어제 MVP를 각각 조회해 묶어서 반환한다")
    void shouldReturnTodayAndYesterdayMvp() {
        LocalDate today = LocalDate.now(KST);
        LocalDate yesterday = today.minusDays(1);

        DailyMvpResponse.Daily todayMvp = DailyMvpResponse.Daily.builder().memberId(10L).build();
        DailyMvpResponse.Daily yesterdayMvp = DailyMvpResponse.Daily.builder().memberId(20L).build();

        when(getDailyMvpUseCase.execute(today)).thenReturn(todayMvp);
        when(getDailyMvpUseCase.execute(yesterday)).thenReturn(yesterdayMvp);

        DailyMvpResponse.DailyWithYesterday result = getTodayAndYesterdayMvpUseCase.execute();

        assertSame(todayMvp, result.getToday());
        assertSame(yesterdayMvp, result.getYesterday());
        verify(getDailyMvpUseCase).execute(today);
        verify(getDailyMvpUseCase).execute(yesterday);
    }

    @Test
    @DisplayName("한쪽 데이터가 없어도 null-safe로 반환한다")
    void shouldReturnNullSafelyWhenOneSideMissing() {
        LocalDate today = LocalDate.now(KST);
        LocalDate yesterday = today.minusDays(1);

        DailyMvpResponse.Daily todayMvp = DailyMvpResponse.Daily.builder().memberId(10L).build();

        when(getDailyMvpUseCase.execute(today)).thenReturn(todayMvp);
        when(getDailyMvpUseCase.execute(yesterday)).thenReturn(null);

        DailyMvpResponse.DailyWithYesterday result = getTodayAndYesterdayMvpUseCase.execute();

        assertSame(todayMvp, result.getToday());
        assertNull(result.getYesterday());
    }
}
