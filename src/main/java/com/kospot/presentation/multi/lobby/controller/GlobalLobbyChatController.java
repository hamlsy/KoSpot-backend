package com.kospot.presentation.multi.lobby.controller;

import com.kospot.application.lobby.http.usecase.JoinGlobalLobbyUseCase;
import com.kospot.application.lobby.http.usecase.LeaveGlobalLobbyUseCase;
import com.kospot.application.lobby.websocket.usecase.SendGlobalLobbyMessageUseCase;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Global Lobby Chat Websocket", description = "글로벌 로비 채팅 소켓")
public class GlobalLobbyChatController {

    private final JoinGlobalLobbyUseCase joinGlobalLobbyUseCase;
    private final SendGlobalLobbyMessageUseCase sendGlobalLobbyMessageUseCase;

    @MessageMapping("/chat.message.lobby")
    public void sendGlobalMessage(@Valid @Payload ChatMessageDto.Lobby dto, SimpMessageHeaderAccessor headerAccessor) {
        sendGlobalLobbyMessageUseCase.execute(dto, headerAccessor);
    }

    @SubscribeMapping("/chat/lobby")
    public void joinGlobalLobby(SimpMessageHeaderAccessor headerAccessor) {
        joinGlobalLobbyUseCase.execute(headerAccessor);
    }

}
