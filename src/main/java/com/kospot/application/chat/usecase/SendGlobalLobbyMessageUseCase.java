package com.kospot.application.chat.usecase;

import com.kospot.application.chat.command.SendGlobalLobbyMessageCommand;
import com.kospot.domain.chat.entity.ChatMessage;
import com.kospot.domain.chat.service.ChatService;
import com.kospot.domain.chat.vo.ChannelType;
import com.kospot.domain.chat.vo.MessageType;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.exception.object.domain.ChatHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.websocket.auth.ChatMemberPrincipal;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.security.Principal;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class SendGlobalLobbyMessageUseCase {

    private final ChatService chatService;

    @Async("chatRoomExecutor")
    public void execute(ChatMessageDto dto, Principal principal) {
        ChatMemberPrincipal chatMemberPrincipal = (ChatMemberPrincipal) principal;
        SendGlobalLobbyMessageCommand command = SendGlobalLobbyMessageCommand.from(dto, chatMemberPrincipal);
        validateCommand(command);
        ChatMessage chatMessage = createChatMessage(command);
        chatService.sendGlobalLobbyMessage(chatMessage);
    }

    private ChatMessage createChatMessage(SendGlobalLobbyMessageCommand command) {
        return ChatMessage.builder()
                .memberId(command.getMemberId())
                .nickname(command.getNickname())
                .messageType(MessageType.GLOBAL_CHAT)
                .channelType(ChannelType.GLOBAL_LOBBY)
                .content(command.getContent())
                .build();
    }

    private void validateCommand(SendGlobalLobbyMessageCommand command) {
        if (command.getContent() == null || command.getContent().isEmpty()) {
            throw new ChatHandler(ErrorStatus.CHAT_MESSAGE_CONTENT_EMPTY);
        }
        if (command.getChannelType() == null || !ChannelType.GLOBAL_LOBBY.equals(command.getChannelType())) {
            throw new ChatHandler(ErrorStatus.CHAT_INVALID_CHANNEL_TYPE);
        }
        // todo Additional validations
    }
}
