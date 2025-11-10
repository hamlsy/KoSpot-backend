package com.kospot.presentation.chat.dto.request;

import jakarta.validation.constraints.NotEmpty;
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
        @NotEmpty
        private String content;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GameRoom {
        @NotEmpty
        private String content;
        private String team;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GlobalGame {
        @NotEmpty
        private String content;
    }

}
