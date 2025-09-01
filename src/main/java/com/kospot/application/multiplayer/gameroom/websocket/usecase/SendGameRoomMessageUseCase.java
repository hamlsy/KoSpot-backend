package com.kospot.application.multiplayer.gameroom.websocket.usecase;

import com.kospot.application.multiplayer.gameroom.websocket.command.SendGameRoomMessageCommand;
import com.kospot.domain.chat.entity.ChatMessage;
import com.kospot.domain.chat.service.ChatService;
import com.kospot.domain.chat.vo.ChannelType;
import com.kospot.domain.chat.vo.MessageType;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.exception.object.domain.WebSocketHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.scheduling.annotation.Async;

@UseCase
@RequiredArgsConstructor
public class SendGameRoomMessageUseCase {

    private final ChatService chatService;

    @Async("chatRoomExecutor")
    public void execute(String roomId, ChatMessageDto.GameRoom dto, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal webSocketMemberPrincipal = (WebSocketMemberPrincipal) headerAccessor.getSessionAttributes().get("user");
        SendGameRoomMessageCommand command = SendGameRoomMessageCommand.from(roomId, dto, webSocketMemberPrincipal);
        validateCommand(command);
        ChatMessage chatMessage = createGameRoomChatMessage(command);
        chatService.sendGameRoomMessage(chatMessage);
    }

    private ChatMessage createGameRoomChatMessage(SendGameRoomMessageCommand command) {
        return ChatMessage.builder()
                .memberId(command.getMemberId())
                .nickname(command.getNickname())
                .messageType(MessageType.GAME_ROOM_CHAT)
                .gameRoomId(command.getGameRoomId())
                .content(command.getContent())
                .build();
    }

    private void validateCommand(SendGameRoomMessageCommand command) {
        validateContent(command);
        // todo Additional validations
    }

    private void validateContent(SendGameRoomMessageCommand command) {
        if (command.getContent() == null || command.getContent().isEmpty()) {
            throw new WebSocketHandler(ErrorStatus.CHAT_MESSAGE_CONTENT_EMPTY);
        }
    }
}
