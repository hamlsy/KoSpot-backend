package com.kospot.multi.submission.presentation.dto.response;

import com.kospot.multi.submission.entity.roadview.RoadViewSubmission;
import lombok.*;

public class SubmissionResponse {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class RoadViewPlayer {
        private String nickname;
        private Double lat;
        private Double lng;
        private Double distance;
        private Double timeToAnswer;
        private double earnedScore;

        public static RoadViewPlayer from(RoadViewSubmission submission) {
            return RoadViewPlayer.builder() // fetch
                    .nickname(submission.getGamePlayer().getNickname())
                    .lat(submission.getLat())
                    .lng(submission.getLng())
                    .distance(submission.getDistance())
                    .timeToAnswer(submission.getTimeToAnswer())
                    .earnedScore(submission.getEarnedScore())
                    .build();
        }
    }
}
