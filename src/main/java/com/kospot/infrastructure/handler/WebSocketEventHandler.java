package com.kospot.infrastructure.handler;

import com.kospot.domain.multiGame.gameRoom.service.GameRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventHandler {

    private final GameRoomService gameRoomService;
    private final RedisTemplate<String, Object> redisTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // 세션 정보를 Redis에 저장 (다중 서버 환경 대응)
        String sessionKey = "websocket:session:" + sessionId;
        redisTemplate.opsForValue().set(sessionKey, System.currentTimeMillis(), Duration.ofHours(2));

        log.info("WebSocket 연결 성공 - SessionId: {}", sessionId);
    }

    // 클라이언트 연결 해제 시 처리
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        try {
            // todo 세션에서 사용자 정보 추출하여 게임방에서 제거
//            gameRoomService.handlePlayerDisconnect(sessionId);

            // Redis에서 세션 정보 삭제
            redisTemplate.delete("websocket:session:" + sessionId);

        } catch (Exception e) {
            log.error("WebSocket 연결 해제 처리 중 오류 발생 - SessionId: {}", sessionId, e);
        }

        log.info("WebSocket 연결 해제 - SessionId: {}", sessionId);
    }

}
