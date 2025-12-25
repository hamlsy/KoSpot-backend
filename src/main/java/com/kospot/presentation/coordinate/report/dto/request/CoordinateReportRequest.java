package com.kospot.presentation.coordinate.report.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CoordinateReportRequest {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Report {

        private Long coordinateId;
        private String reason;
        private String detail;

    }
}
