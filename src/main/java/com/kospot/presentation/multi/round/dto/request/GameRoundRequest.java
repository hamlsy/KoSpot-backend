package com.kospot.presentation.multi.round.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

public class GameRoundRequest {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Start {
        private Long gameRoomId;
        @Min(10) // 10 seconds
        @Max(180) // 3 minutes
        private Integer timeLimit;
    }
}
