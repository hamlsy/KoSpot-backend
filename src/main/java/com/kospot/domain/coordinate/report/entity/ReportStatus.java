package com.kospot.domain.coordinate.report.entity;

public enum ReportStatus {
    PENDING,   // 접수됨(관리자 확인 대기)
    RESOLVED,  // 처리됨(좌표 조치/확인 완료)
    REJECTED   // 반려됨(문제 없음/오신고)
}
