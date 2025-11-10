package com.kospot.application.multi.game.websocket.command;

import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SendSoloGameMessageCommand {
    private Long gameRoomId;
    private Long memberId;
    private String nickname;
    private String content;

    public static SendSoloGameMessageCommand from(String roomId, ChatMessageDto.GlobalGame dto, WebSocketMemberPrincipal principal) {
        return SendSoloGameMessageCommand.builder()
                .memberId(principal.getMemberId())
                .nickname(principal.getNickname())
                .gameRoomId(Long.parseLong(roomId))
                .content(dto.getContent())
                .build();
    }
}
