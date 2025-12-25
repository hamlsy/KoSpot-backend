package com.kospot.domain.coordinate.report.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Table(
        name = "coordinate_report",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coordinate_report_coordinate_reporter",
                        columnNames = {"coordinate_id", "reporter_member_id"}
                )
        },
        indexes = {
                @Index(name = "idx_coordinate_report_coordinate", columnList = "coordinate_id"),
                @Index(name = "idx_coordinate_report_reporter", columnList = "reporter_member_id"),
        }
)
public class CoordinateReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신고된 좌표 */
    @Column(name = "coordinate_id", nullable = false)
    private Long coordinateId;

    /** 신고자(회원) */
    @Column(name = "reporter_member_id", nullable = false)
    private Long reporterMemberId;

    /** 신고 사유(원클릭 + 기타) */
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReportReason reason;

    /** 기타 사유 상세(OTHER일 때만 사용) */
    @Lob
    @Column(name = "detail", length = 500)
    private String detail;

    /** 처리 상태(관리자 검토/처리) */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // ---------- lifecycle ----------

    @PrePersist
    public void prePersist() {
        if (this.status == null) this.status = ReportStatus.PENDING;
        // reason != OTHER 인 경우 detail을 저장하지 않도록 정리(원클릭 UX)
        if (this.reason != ReportReason.OTHER) this.detail = null;
    }

    // ---------- domain methods (optional) ----------

    public void resolve() {
        this.status = ReportStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = ReportStatus.REJECTED;
        this.resolvedAt = LocalDateTime.now();
    }


    // 생성자 메서드
    public static CoordinateReport create(
            Long coordinateId,
            Long reporterMemberId,
            ReportReason reason,
            String detail
    ) {
        CoordinateReport report = new CoordinateReport();
        report.coordinateId = coordinateId;
        report.reporterMemberId = reporterMemberId;
        report.reason = reason;
        report.detail = detail;
        return report;
    }

}