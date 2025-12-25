package com.kospot.application.coordinate.report;

import com.kospot.domain.coordinate.report.service.CoordinateReportService;
import com.kospot.domain.member.entity.Member;
import com.kospot.presentation.coordinate.report.dto.request.CoordinateReportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@RequiredArgsConstructor
public class ReportCoordinateUseCase {

    private final CoordinateReportService coordinateReportService;

    public void execute(CoordinateReportRequest.Report request, Member member) {
        Long coordinateId = request.getCoordinateId();
        Long memberId = member.getId();
        String reason = request.getReason();
        String detail = request.getDetail();
        coordinateReportService.reportCoordinate(coordinateId, memberId, reason, detail);
    }

}
