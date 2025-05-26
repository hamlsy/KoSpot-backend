package com.kospot.presentation.multiGame.submission.dto.response;

import com.kospot.domain.multiGame.submission.entity.roadView.RoadViewPlayerSubmission;
import lombok.*;

public class SubmissionResponse {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class RoadViewPlayer {
        private Double lat;
        private Double lng;
        private Double distance;
        private Double timeToAnswer;
        private Integer score;

        public static RoadViewPlayer from(RoadViewPlayerSubmission submission) {
            return RoadViewPlayer.builder()
                    .lat(submission.getLat())
                    .lng(submission.getLng())
                    .distance(submission.getDistance())
                    .timeToAnswer(submission.getTimeToAnswer())
                    .score(submission.getScore())
                    .build();
        }
    }
}
