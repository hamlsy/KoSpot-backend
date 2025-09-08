package com.kospot.presentation.chat.dto.event;

import com.kospot.domain.chat.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ChatMessageEvent {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GlobalLobby {

        private Long senderId;
        private String messageId;
        private String nickname;
        private String content;
        private String messageType;
        private LocalDateTime timestamp;

        public static GlobalLobby from(ChatMessage chatMessage) {
            return GlobalLobby.builder()
                    .senderId(chatMessage.getMemberId())
                    .messageId(chatMessage.getMessageId())
                    .nickname(chatMessage.getNickname())
                    .content(chatMessage.getContent())
                    .messageType(chatMessage.getMessageType().name())
                    .timestamp(chatMessage.getCreatedDate())
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GameRoom {

        private Long senderId;
        private String messageId;
        private String nickname;
        private String content;
        private String messageType;
        private String teamId;
        private LocalDateTime timestamp;

        public static GameRoom from(ChatMessage chatMessage) {
            return GameRoom.builder()
                    .senderId(chatMessage.getMemberId())
                    .messageId(chatMessage.getMessageId())
                    .nickname(chatMessage.getNickname())
                    .content(chatMessage.getContent())
                    .messageType(chatMessage.getMessageType().name())
                    .teamId(chatMessage.getTeamId())
                    .timestamp(chatMessage.getCreatedDate())
                    .build();
        }
    }

}
