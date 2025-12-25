package com.kospot.domain.coordinate.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {
    VIEW_UNAVAILABLE, // 좌표(로드뷰)가 안뜸/접근 불가
    NO_EVIDENCE,      // 근거 부족(단서 없음)
    TOO_DIFFICULT,    // 난이도가 너무 어려움
    OTHER;             // 기타(상세 필수 - 서비스에서 검증)

    public static ReportReason fromKey(String key) {
        for (ReportReason reason : ReportReason.values()) {
            if (reason.name().equalsIgnoreCase(key)) {
                return reason;
            }
        }
        throw new IllegalArgumentException("Invalid ReportReason key: " + key);
    }

}
