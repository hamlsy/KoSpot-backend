package com.kospot.presentation.admin.controller;

import com.kospot.application.admin.statistics.GetOverallStatisticsUseCase;
import com.kospot.application.admin.statistics.GetStatisticsByPeriodUseCase;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.admin.dto.response.GameModeStatisticSummary;
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

    @Operation(summary = "전체 통계 조회", description = "게임 모드별 전체 통계를 조회합니다.")
    @GetMapping("/overall")
    public ApiResponseDto<List<GameModeStatisticSummary>> getOverallStatistics(
            @CurrentMember com.kospot.domain.member.entity.Member admin) {
        admin.validateAdmin();
        List<GameModeStatisticSummary> statistics = getOverallStatisticsUseCase.execute();
        return ApiResponseDto.onSuccess(statistics);
    }

    @Operation(summary = "기간별 통계 조회", description = "일간/주간/월간 통계를 조회합니다.")
    @GetMapping("/period/{period}")
    public ApiResponseDto<List<GameModeStatisticSummary>> getStatisticsByPeriod(
            @CurrentMember com.kospot.domain.member.entity.Member admin,
            @PathVariable String period) {
        admin.validateAdmin();
        List<GameModeStatisticSummary> statistics = getStatisticsByPeriodUseCase.execute(period);
        return ApiResponseDto.onSuccess(statistics);
    }
}

