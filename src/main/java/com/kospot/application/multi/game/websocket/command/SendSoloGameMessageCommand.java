package com.kospot.application.multi.game.websocket.command;

import com.kospot.infrastructure.redis.domain.member.adaptor.MemberProfileRedisAdaptor;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SendSoloGameMessageCommand {
    private Long playerId;
    private Long gameRoomId;
    private Long memberId;
    private String nickname;
    private String content;
    private String markerImageUrl;

    public static SendSoloGameMessageCommand from(String roomId, ChatMessageDto.GlobalGame dto, MemberProfileRedisAdaptor.MemberProfileView profileView) {
        return SendSoloGameMessageCommand.builder()
                .playerId(dto.getPlayerId())
                .memberId(profileView.memberId())
                .nickname(profileView.nickname())
                .markerImageUrl(profileView.markerImageUrl())
                .gameRoomId(Long.parseLong(roomId))
                .content(dto.getContent())
                .build();
    }
}
