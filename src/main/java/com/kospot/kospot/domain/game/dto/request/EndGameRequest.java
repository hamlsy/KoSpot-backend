package com.kospot.kospot.domain.game.dto.request;

import lombok.*;

public class EndGameRequest {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoadView {
        private Long gameId;
        private double submittedLat;
        private double submittedLng;
        private double answerDistance;
        private double answerTime;
    }
}
