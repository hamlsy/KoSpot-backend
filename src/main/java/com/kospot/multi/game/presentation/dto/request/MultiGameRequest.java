package com.kospot.multi.game.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

public class MultiGameRequest {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Start {

        private String gameModeKey;
        private String playerMatchTypeKey;
        private Integer timeLimit;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Reissue {

        @NotNull
        private Long expectedRoundVersion;

        private String reason;
    }

}
