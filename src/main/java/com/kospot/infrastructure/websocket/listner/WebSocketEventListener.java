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

    // todo 클라이언트 연결 해제 시 처리, 분기처리하기
    // leave global lobby, leave game room, etc.
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        leaveGlobalLobbyUseCase.execute(headerAccessor);
        log.info("Session disconnected: {}", event.getMessage());
    }

}
