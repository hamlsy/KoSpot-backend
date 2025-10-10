package com.kospot.presentation.multi.submission.dto.request;

import com.kospot.domain.multi.submission.entity.roadView.RoadViewPlayerSubmission;
import lombok.*;

public class SubmitRoadViewRequest {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Player {
        
        private Long playerId;
        private Double lat;
        private Double lng;
        private Double distance;
        private Double timeToAnswer;

        public RoadViewPlayerSubmission toEntity() {
            return RoadViewPlayerSubmission.builder()
                    .lat(lat)
                    .lng(lng)
                    .distance(distance)
                    .timeToAnswer(timeToAnswer)
                    .build();
        }

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Team {

        private Long teamId;
        private Double lat;
        private Double lng;
        private Double distance;

    }


}
