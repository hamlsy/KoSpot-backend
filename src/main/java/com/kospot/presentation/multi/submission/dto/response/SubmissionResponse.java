package com.kospot.presentation.multi.submission.dto.response;

import com.kospot.domain.multi.submission.entity.roadview.RoadViewSubmission;
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
        private double earnedScore;

        public static RoadViewPlayer from(RoadViewSubmission submission) {
            return RoadViewPlayer.builder()
                    .lat(submission.getLat())
                    .lng(submission.getLng())
                    .distance(submission.getDistance())
                    .timeToAnswer(submission.getTimeToAnswer())
                    .earnedScore(submission.getEarnedScore())
                    .build();
        }
    }
}
