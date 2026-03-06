package com.kospot.admin.presentation.controller;

import com.kospot.admin.application.usecase.statistics.GetOverallStatisticsUseCase;
import com.kospot.admin.application.usecase.statistics.GetStatisticsByPeriodUseCase;
import com.kospot.admin.application.usecase.access.ValidateAdminUseCase;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
import com.kospot.common.security.aop.CurrentMember;
import com.kospot.admin.presentation.dto.response.GameModeStatisticSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Statistics", description = "관리자 통계 API")
@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final GetOverallStatisticsUseCase getOverallStatisticsUseCase;
    private final GetStatisticsByPeriodUseCase getStatisticsByPeriodUseCase;
    private final ValidateAdminUseCase validateAdminUseCase;

    @Operation(summary = "전체 통계 조회", description = "게임 모드별 전체 통계를 조회합니다.")
    @GetMapping("/overall")
    public ApiResponseDto<List<GameModeStatisticSummary>> getOverallStatistics(
            @CurrentMember Long adminId) {
        validateAdminUseCase.execute(adminId);
        List<GameModeStatisticSummary> statistics = getOverallStatisticsUseCase.execute();
        return ApiResponseDto.onSuccess(statistics);
    }

    @Operation(summary = "기간별 통계 조회", description = "일간/주간/월간 통계를 조회합니다.")
    @GetMapping("/period/{period}")
    public ApiResponseDto<List<GameModeStatisticSummary>> getStatisticsByPeriod(
            @CurrentMember Long adminId,
            @PathVariable String period) {
        validateAdminUseCase.execute(adminId);
        List<GameModeStatisticSummary> statistics = getStatisticsByPeriodUseCase.execute(period);
        return ApiResponseDto.onSuccess(statistics);
    }
}

