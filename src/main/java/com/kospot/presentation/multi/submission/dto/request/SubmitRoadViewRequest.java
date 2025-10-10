package com.kospot.presentation.multi.submission.dto.request;

import com.kospot.domain.multi.submission.entity.roadView.RoadViewPlayerSubmission;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

public class SubmitRoadViewRequest {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Player {

        @NotNull(message = "위도는 필수입니다")
        private Double lat;

        @NotNull(message = "경도는 필수입니다")
        private Double lng;

        @NotNull(message = "거리는 필수입니다")
        @Positive(message = "거리는 0보다 커야 합니다")
        private Double distance;

        @NotNull(message = "응답 시간은 필수입니다")
        @Positive(message = "응답 시간은 0보다 커야 합니다")
        private Double timeToAnswer; // 밀리초

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
