package com.kospot.presentation.multiGame.submission.dto.request;

import lombok.*;

public class SubmissionRequest {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class RoadView {
        
        private Long playerId;
        private Double lat;
        private Double lng;
        private Double distance;

    }

}
