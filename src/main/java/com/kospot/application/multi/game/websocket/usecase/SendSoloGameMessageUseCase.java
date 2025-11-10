package com.kospot.application.multi.game.websocket.usecase;

import com.kospot.application.multi.game.websocket.command.SendSoloGameMessageCommand;
import com.kospot.domain.chat.entity.ChatMessage;
import com.kospot.domain.chat.service.ChatService;
import com.kospot.domain.chat.vo.MessageType;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
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

    private final ChatService chatService;

    @Async("chatRoomExecutor")
    public void execute(String roomId, ChatMessageDto.GlobalGame dto, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal webSocketMemberPrincipal = WebSocketMemberPrincipal.getPrincipal(headerAccessor);
        SendSoloGameMessageCommand command = SendSoloGameMessageCommand.from(roomId, dto, webSocketMemberPrincipal);
        validateCommand(command);
        ChatMessage chatMessage = createGlobalChatMessage(command);
        chatService.sendSoloGameMessage(chatMessage);
    }
    public void execute() {

    }

    private ChatMessage createGlobalChatMessage(SendSoloGameMessageCommand command) {
        return ChatMessage.builder()
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
