package com.kospot.mvp.application.usecase;

import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.mvp.presentation.dto.response.DailyMvpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetTodayAndYesterdayMvpUseCase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GetDailyMvpUseCase getDailyMvpUseCase;

    public DailyMvpResponse.DailyWithYesterday execute() {
        LocalDate today = LocalDate.now(KST);
        LocalDate yesterday = today.minusDays(1);

        DailyMvpResponse.Daily todayMvp = getDailyMvpUseCase.execute(today);
        DailyMvpResponse.Daily yesterdayMvp = getDailyMvpUseCase.execute(yesterday);

        return DailyMvpResponse.DailyWithYesterday.builder()
                .today(todayMvp)
                .yesterday(yesterdayMvp)
                .build();
    }
}
