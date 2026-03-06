package com.kospot.mvp.presentation.controller;

import com.kospot.mvp.application.usecase.GetDailyMvpUseCase;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.mvp.presentation.response.DailyMvpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "MVP Api", description = "오늘의 MVP API")
@RequestMapping("/mvps")
public class DailyMvpController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GetDailyMvpUseCase getDailyMvpUseCase;

    @Operation(summary = "일자별 MVP 조회", description = "특정 날짜의 오늘의 MVP 정보를 조회합니다.")
    @GetMapping("/daily")
    public ApiResponseDto<DailyMvpResponse.Daily> getDailyMvp(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now(KST);
        return ApiResponseDto.onSuccess(getDailyMvpUseCase.execute(targetDate));
    }
}
