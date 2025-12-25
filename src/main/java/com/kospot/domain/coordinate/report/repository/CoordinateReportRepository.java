package com.kospot.domain.coordinate.report.repository;

import com.kospot.domain.coordinate.report.entity.CoordinateReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CoordinateReportRepository extends JpaRepository<CoordinateReport, Long> {


    List<CoordinateReport> findByCoordinateId(Long coordinateId);

    @Query("SELECT cr FROM CoordinateReport cr WHERE cr.reporterMemberId = :memberId ORDER BY cr.createdDate DESC")
    List<CoordinateReport> findAllByMemberId(@Param("memberId") Long memberId);

    boolean existsByReporterMemberIdAndCoordinateId(Long reporterMemberId, Long coordinateId);

}
