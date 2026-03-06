package com.kospot.chat.presentation.dto.event;

import com.kospot.chat.domain.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

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
                    .timestamp(resolveTimestamp(chatMessage))
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
                    .timestamp(resolveTimestamp(chatMessage))
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MultiGameGlobal {

        private Long senderId;
        private String messageId;
        private String nickname;
        private String content;
        private String messageType;
        private LocalDateTime timestamp;

        public static MultiGameGlobal from(ChatMessage chatMessage) {
            return MultiGameGlobal.builder()
                    .senderId(chatMessage.getMemberId())
                    .messageId(chatMessage.getMessageId())
                    .nickname(chatMessage.getNickname())
                    .content(chatMessage.getContent())
                    .messageType(chatMessage.getMessageType().name())
                    .timestamp(resolveTimestamp(chatMessage))
                    .build();
        }
    }

    private static LocalDateTime resolveTimestamp(ChatMessage chatMessage) {
        if (chatMessage.getCreatedDate() != null) {
            return chatMessage.getCreatedDate();
        }
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

}
