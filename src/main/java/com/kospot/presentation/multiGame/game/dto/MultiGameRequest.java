package com.kospot.presentation.multiGame.game.dto;

import lombok.*;

import java.util.List;

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
