package com.kospot.application.multi.room.websocket.command;

import com.kospot.infrastructure.redis.domain.member.adaptor.MemberProfileRedisAdaptor;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SendGameRoomMessageCommand {

    private Long gameRoomId;
    private Long memberId;
    private String nickname;
    private String content;

    public static SendGameRoomMessageCommand from(String roomId, ChatMessageDto.GameRoom dto, MemberProfileRedisAdaptor.MemberProfileView profileView) {
        return SendGameRoomMessageCommand.builder()
                .memberId(profileView.memberId())
                .nickname(profileView.nickname())
                .gameRoomId(Long.parseLong(roomId))
                .content(dto.getContent())
                .build();
    }

}
