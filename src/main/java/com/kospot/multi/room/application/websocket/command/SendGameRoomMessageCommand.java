package com.kospot.multi.room.application.websocket.command;

import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.chat.presentation.dto.request.ChatMessageDto;
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
