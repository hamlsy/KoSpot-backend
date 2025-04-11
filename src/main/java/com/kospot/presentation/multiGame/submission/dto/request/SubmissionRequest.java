package com.kospot.presentation.multiGame.submission.dto.request;

import lombok.*;

public class SubmissionRequest {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class RoadViewPlayer {
        
        private Long playerId;
        private Double lat;
        private Double lng;
        private Double distance;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class RoadViewTeam {

        private Long teamId;
        private Double lat;
        private Double lng;
        private Double distance;

    }


}
