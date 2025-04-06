package com.kospot.presentation.multiGame.game.dto.request;

import lombok.*;

public class MultiGameRequest {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Start {

        private Long gameRoomId;
        private String gameModeKey;
        private String playerMatchTypeKey;
        private int roundCount;

    }
}
