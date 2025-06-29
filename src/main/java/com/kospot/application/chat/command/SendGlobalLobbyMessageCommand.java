package com.kospot.application.chat.command;

import com.kospot.infrastructure.websocket.auth.ChatMemberPrincipal;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SendGlobalLobbyMessageCommand {

    private Long memberId;
    private String nickname;
    private String messageType;
    private String channelType;
    private String content;
    private LocalDateTime timeStamp;

    public static SendGlobalLobbyMessageCommand from(ChatMessageDto dto, ChatMemberPrincipal principal) {
        return SendGlobalLobbyMessageCommand.builder()
                .memberId(principal.getMemberId())
                .nickname(principal.getNickname())
                .messageType(dto.getMessageType())
                .channelType(dto.getChannelType())
                .content(dto.getContent())
                .timeStamp(LocalDateTime.now())
                .build();
    }

}
