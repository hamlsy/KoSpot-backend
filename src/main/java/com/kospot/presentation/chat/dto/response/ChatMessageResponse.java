package com.kospot.presentation.chat.dto.response;

import com.kospot.domain.chat.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ChatMessageResponse {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GlobalLobby {

        private Long senderId;
        private String messageId;
        private String nickname;
        private String content;
        private String channelType;
        private String messageType;
        private LocalDateTime timestamp;

        public static GlobalLobby from(ChatMessage chatMessage) {
            return GlobalLobby.builder()
                    .senderId(chatMessage.getMemberId())
                    .messageId(chatMessage.getMessageId())
                    .nickname(chatMessage.getNickname())
                    .content(chatMessage.getContent())
                    .channelType(chatMessage.getChannelType().name())
                    .messageType(chatMessage.getMessageType().name())
                    .timestamp(chatMessage.getCreatedDate())
                    .build();
        }
    }

}
