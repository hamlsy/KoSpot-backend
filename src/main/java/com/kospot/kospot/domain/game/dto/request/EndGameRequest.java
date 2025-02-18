package com.kospot.kospot.domain.game.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class EndGameRequest {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoadViewPractice{
        private Long gameId;
        private double submittedLat;
        private double submittedLng;
        private double answerDistance;
        private double answerTime;
    }
}
