package com.kospot.domain.coordinate.report.adaptor;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.report.entity.CoordinateReport;
import com.kospot.domain.coordinate.report.repository.CoordinateReportRepository;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
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
