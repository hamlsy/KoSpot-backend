package com.kospot.multi.game.presentation.controller;

import com.kospot.multi.common.flow.GameTransitionOrchestrator;
import com.kospot.multi.game.application.websocket.usecase.SendSoloGameMessageUseCase;
import com.kospot.chat.presentation.dto.request.ChatMessageDto;
import com.kospot.multi.game.presentation.dto.message.LoadingAckMessage;
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

    private final GameTransitionOrchestrator gameTransitionOrchestrator;

    private final SendSoloGameMessageUseCase sendSoloGameMessageUseCase;

    @MessageMapping("/room.{roomId}.loading.ack")
    public void handleLoadingAck(@DestinationVariable("roomId") String roomId,
                                 @Payload LoadingAckMessage message,
                                 SimpMessageHeaderAccessor headerAccessor) {
        gameTransitionOrchestrator.handleLoadingAck(roomId, message, headerAccessor);
    }

    @MessageMapping("/room.{roomId}.game.global.chat")
    public void sendGameChat(@DestinationVariable("roomId") String roomId,
                             @Payload ChatMessageDto.GlobalGame chatMessage,
                             SimpMessageHeaderAccessor headerAccessor) {
        sendSoloGameMessageUseCase.execute(roomId, chatMessage, headerAccessor);
    }

}
