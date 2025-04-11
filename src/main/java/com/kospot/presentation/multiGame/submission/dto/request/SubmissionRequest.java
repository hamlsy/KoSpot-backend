package com.kospot.presentation.multiGame.submission.dto.request;

import com.kospot.domain.multiGame.submission.entity.roadView.RoadViewPlayerSubmission;
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

        public RoadViewPlayerSubmission toEntity() {
            return RoadViewPlayerSubmission.builder()
                    .latitude(lat)
                    .longitude(lng)
                    .distance(distance)
                    .build();
        }

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
