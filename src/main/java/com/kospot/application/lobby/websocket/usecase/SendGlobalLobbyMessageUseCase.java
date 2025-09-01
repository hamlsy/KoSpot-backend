package com.kospot.application.lobby.websocket.usecase;

import com.kospot.application.lobby.websocket.command.SendGlobalLobbyMessageCommand;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.scheduling.annotation.Async;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class SendGlobalLobbyMessageUseCase {

    private final ChatService chatService;

    @Async("chatRoomExecutor")
    public void execute(ChatMessageDto.Lobby dto, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal webSocketMemberPrincipal = (WebSocketMemberPrincipal) headerAccessor.getSessionAttributes().get("user");
        SendGlobalLobbyMessageCommand command = SendGlobalLobbyMessageCommand.from(dto, webSocketMemberPrincipal);
        validateCommand(command);
        ChatMessage chatMessage = createChatMessage(command);
        chatService.sendGlobalLobbyMessage(chatMessage);
    }

    private ChatMessage createChatMessage(SendGlobalLobbyMessageCommand command) {
        return ChatMessage.builder()
                .memberId(command.getMemberId())
                .nickname(command.getNickname())
                .messageType(MessageType.GLOBAL_CHAT)
                .content(command.getContent())
                .build();
    }

    private void validateCommand(SendGlobalLobbyMessageCommand command) {
        validateContent(command);
        // todo Additional validations
    }

    private void validateContent(SendGlobalLobbyMessageCommand command) {
        if (command.getContent() == null || command.getContent().isEmpty()) {
            throw new WebSocketHandler(ErrorStatus.CHAT_MESSAGE_CONTENT_EMPTY);
        }
    }

}
