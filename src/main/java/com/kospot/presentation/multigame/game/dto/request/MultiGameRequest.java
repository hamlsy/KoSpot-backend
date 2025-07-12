package com.kospot.presentation.multigame.game.dto.request;

import lombok.*;

public class MultiGameRequest {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Start {

        private Long gameRoomId;
        private String playerMatchTypeKey;
        private int totalRounds;

    }


}
