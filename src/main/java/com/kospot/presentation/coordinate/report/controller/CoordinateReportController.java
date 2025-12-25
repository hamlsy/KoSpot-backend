package com.kospot.presentation.coordinate.report.controller;

import com.kospot.application.coordinate.report.ReportCoordinateUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.coordinate.report.dto.request.CoordinateReportRequest;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Coordinate Report Api", description = "좌표 신고 API")
@RequestMapping("/coordinate-report")
public class CoordinateReportController {

    private final ReportCoordinateUseCase reportCoordinateUseCase;

    @PostMapping
    public ApiResponseDto<?> reportCoordinate(@RequestBody CoordinateReportRequest.Report request, @CurrentMember Member member) {
        reportCoordinateUseCase.execute(request, member);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}
