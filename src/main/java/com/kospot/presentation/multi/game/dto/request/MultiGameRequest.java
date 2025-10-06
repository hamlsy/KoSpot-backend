package com.kospot.presentation.multi.game.dto.request;

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
        private Integer timeLimit;

    }


}
