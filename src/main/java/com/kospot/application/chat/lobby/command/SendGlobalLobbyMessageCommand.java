package com.kospot.application.chat.lobby.command;

import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SendGlobalLobbyMessageCommand {

    private Long memberId;
    private String nickname;
    private String content;

    public static SendGlobalLobbyMessageCommand from(ChatMessageDto dto, WebSocketMemberPrincipal principal) {
        return SendGlobalLobbyMessageCommand.builder()
                .memberId(principal.getMemberId())
                .nickname(principal.getNickname())
                .content(dto.getContent())
                .build();
    }

}
