package com.kospot.application.lobby.websocket.command;

import com.kospot.infrastructure.redis.domain.member.adaptor.MemberProfileRedisAdaptor;
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

    public static SendGlobalLobbyMessageCommand from(ChatMessageDto.Lobby dto, MemberProfileRedisAdaptor.MemberProfileView profileView) {
        return SendGlobalLobbyMessageCommand.builder()
                .memberId(profileView.memberId())
                .nickname(profileView.nickname())
                .content(dto.getContent())
                .build();
    }

}
