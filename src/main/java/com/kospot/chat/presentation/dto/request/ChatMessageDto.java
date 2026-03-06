package com.kospot.chat.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


public class ChatMessageDto {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Lobby {
        @NotBlank
        private String content;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GameRoom {
        @NotBlank
        private String content;
        private String team;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GlobalGame {
        private Long playerId;
        @NotBlank
        private String content;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Friend {
        @NotBlank
        private String content;
    }

}
