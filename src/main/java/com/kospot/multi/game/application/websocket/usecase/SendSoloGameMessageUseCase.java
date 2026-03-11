package com.kospot.multi.game.application.websocket.usecase;

import com.kospot.multi.game.application.websocket.command.SendSoloGameMessageCommand;
import com.kospot.chat.domain.entity.ChatMessage;
import com.kospot.chat.application.service.ChatService;
import com.kospot.chat.domain.vo.MessageType;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.common.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.chat.presentation.dto.request.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class SendSoloGameMessageUseCase {

    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;
    private final ChatService chatService;

    @Async("chatRoomExecutor")
    public void execute(String roomId, ChatMessageDto.GlobalGame dto, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal webSocketMemberPrincipal = WebSocketMemberPrincipal.getPrincipal(headerAccessor);
        MemberProfileRedisAdaptor.MemberProfileView memberProfileView =
                memberProfileRedisAdaptor.findProfile(webSocketMemberPrincipal.getMemberId());

        SendSoloGameMessageCommand command = SendSoloGameMessageCommand.from(roomId, dto, memberProfileView);
        validateCommand(command);
        ChatMessage chatMessage = createGlobalChatMessage(command);
        chatService.sendSoloGameMessage(chatMessage);
    }

    private ChatMessage createGlobalChatMessage(SendSoloGameMessageCommand command) {
        return ChatMessage.builder()
                .gamePlayerId(command.getPlayerId())
                .memberId(command.getMemberId())
                .nickname(command.getNickname())
                .messageType(MessageType.GAME_CHAT)
                .gameRoomId(command.getGameRoomId())
                .content(command.getContent())
                .build();
    }

    private void validateCommand(SendSoloGameMessageCommand command) {

        // todo Additional validations
    }

}
