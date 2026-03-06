package com.kospot.coordinate.report.application.adaptor;

import com.kospot.coordinate.report.domain.entity.CoordinateReport;
import com.kospot.coordinate.report.infrastrueture.persistence.CoordinateReportRepository;
import com.kospot.common.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Adaptor
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CoordinateReportAdaptor {

    private final CoordinateReportRepository repository;

    public List<CoordinateReport> queryAllByMemberId(Long memberId) {
        return repository.findAllByMemberId(memberId);
    }

}
