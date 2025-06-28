package com.kospot.application.chat.usecase;

import com.kospot.domain.chat.service.ChatService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ChatGlobalLobbyUseCase {

    private final ChatService chatService;

    public void execute(ChatMessageDto dto) {
        chatService.joinGlobalLobby(sessionId);
    }
}
