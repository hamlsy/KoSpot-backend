package com.kospot.application.chat.usecase;

import com.kospot.application.chat.command.SendGlobalLobbyMessageCommand;
import com.kospot.domain.chat.service.ChatService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.auth.ChatMemberPrincipal;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class SendGlobalLobbyMessageUseCase {

    private final ChatService chatService;

    public void execute(ChatMessageDto dto, Principal principal) {
        ChatMemberPrincipal chatMemberPrincipal = (ChatMemberPrincipal) principal;
        SendGlobalLobbyMessageCommand command = SendGlobalLobbyMessageCommand.from(dto, chatMemberPrincipal);



    }
}
