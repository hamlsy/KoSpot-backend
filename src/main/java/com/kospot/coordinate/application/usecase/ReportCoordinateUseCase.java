package com.kospot.coordinate.application.usecase;

import com.kospot.coordinate.report.application.service.CoordinateReportService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.coordinate.report.presentation.request.CoordinateReportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class ReportCoordinateUseCase {

    private final MemberAdaptor memberAdaptor;
    private final CoordinateReportService coordinateReportService;

    public void execute(CoordinateReportRequest.Report request, Long memberId) {
        Member member = memberAdaptor.queryById(memberId);
        Long coordinateId = request.getCoordinateId();
        String reason = request.getReason();
        String detail = request.getDetail();
        coordinateReportService.reportCoordinate(coordinateId, memberId, reason, detail);
    }

}
