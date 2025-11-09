package com.kospot.presentation.multi.game.controller;

import com.kospot.application.multi.flow.PlayerTransitionService;
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

    @MessageMapping("/room/{roomId}/loading/ack")
    public void handleLoadingAck(@DestinationVariable String roomId,
                                 @Payload LoadingAckMessage message,
                                 SimpMessageHeaderAccessor headerAccessor) {
        playerTransitionService.handleLoadingAck(roomId, message, headerAccessor);
    }

}
