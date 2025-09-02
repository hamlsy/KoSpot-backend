package com.kospot.application.multiplayer.gameroom.websocket.command;

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

    public static SendGameRoomMessageCommand from(String roomId, ChatMessageDto.GameRoom dto, WebSocketMemberPrincipal principal) {
        return SendGameRoomMessageCommand.builder()
                .memberId(principal.getMemberId())
                .nickname(principal.getNickname())
                .gameRoomId(Long.parseLong(roomId))
                .content(dto.getContent())
                .build();
    }

}
