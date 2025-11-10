package com.kospot.presentation.multi.game.controller;

import com.kospot.application.multi.flow.PlayerTransitionService;
import com.kospot.application.multi.game.websocket.usecase.SendSoloGameMessageUseCase;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import com.kospot.presentation.multi.game.dto.message.LoadingAckMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MultiGameWebSocketController {

    private final PlayerTransitionService playerTransitionService;

    private final SendSoloGameMessageUseCase sendSoloGameMessageUseCase;

    @MessageMapping("/room.{roomId}.loading.ack")
    public void handleLoadingAck(@DestinationVariable("roomId") String roomId,
                                 @Payload LoadingAckMessage message,
                                 SimpMessageHeaderAccessor headerAccessor) {
        playerTransitionService.handleLoadingAck(roomId, message, headerAccessor);
    }

    @MessageMapping("/room.{roomId}.game.global.chat")
    public void sendGameChat(@DestinationVariable("roomId") String roomId,
                             @Payload ChatMessageDto.GlobalGame chatMessage,
                             SimpMessageHeaderAccessor headerAccessor) {
        sendSoloGameMessageUseCase.execute(roomId, chatMessage, headerAccessor);
    }

}
