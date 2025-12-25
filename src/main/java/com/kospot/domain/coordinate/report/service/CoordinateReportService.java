package com.kospot.domain.coordinate.report.service;

import com.kospot.domain.coordinate.report.entity.CoordinateReport;
import com.kospot.domain.coordinate.report.entity.ReportReason;
import com.kospot.domain.coordinate.report.repository.CoordinateReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CoordinateReportService {

    private final CoordinateReportRepository repository;

    public void reportCoordinate(Long coordinateId, Long memberId, String reason, String detail) {
        validateReport(coordinateId, memberId);
        ReportReason reportReason = ReportReason.fromKey(reason);
        CoordinateReport coordinateReport = CoordinateReport.create(coordinateId, memberId, reportReason, detail);

        repository.save(coordinateReport);
    }

    private void validateReport(Long coordinateId, Long memberId) {
        boolean alreadyReported = repository.existsByReporterMemberIdAndCoordinateId(coordinateId, memberId);
        if (alreadyReported) {
            throw new IllegalArgumentException("이미 신고한 좌표입니다.");
        }
    }

}
