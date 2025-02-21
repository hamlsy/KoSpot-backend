package com.kospot.kospot.domain.game.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class EndGameRequest {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RoadView {
        private Long gameId;
        private double submittedLat;
        private double submittedLng;
        private double answerDistance;
        private double answerTime;
    }
}
