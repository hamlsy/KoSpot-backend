package com.kospot.presentation.multiGame.round.dto.response;

import lombok.*;

public class GameRoundResponse {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class RoadViewInfo {
        private Long roundId;
        private int roundNumber;
        private double targetLat;
        private double targetLng;
    }
}
