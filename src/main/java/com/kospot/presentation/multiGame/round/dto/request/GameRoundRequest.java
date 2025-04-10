package com.kospot.presentation.multiGame.round.dto.request;

import lombok.*;

public class GameRoundRequest {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class NextRound {

        private Long multiGameId;
        private int currentRound;

    }

}
