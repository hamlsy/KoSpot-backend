package com.kospot.application.lobby.http.usecase;

import com.kospot.domain.chat.service.ChatService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class LeaveGlobalLobbyUseCase {

    private final ChatService chatService;

    public void execute(SimpMessageHeaderAccessor headerAccessor) {
        //todo session 기반으로 변경
        chatService.leaveGlobalLobby(headerAccessor.getSessionId());
    }

}
