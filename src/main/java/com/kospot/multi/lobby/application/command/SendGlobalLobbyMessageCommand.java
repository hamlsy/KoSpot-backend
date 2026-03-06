package com.kospot.multi.lobby.application.command;

import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.chat.presentation.dto.request.ChatMessageDto;
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
