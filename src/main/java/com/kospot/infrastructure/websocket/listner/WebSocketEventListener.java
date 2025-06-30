package com.kospot.infrastructure.websocket.listner;

import com.kospot.application.chat.lobby.usecase.LeaveGlobalLobbyUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final LeaveGlobalLobbyUseCase leaveGlobalLobbyUseCase;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        leaveGlobalLobbyUseCase.execute(headerAccessor);
        log.info("Session disconnected: {}", event.getMessage());
    }

}
