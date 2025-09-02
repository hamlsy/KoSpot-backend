package com.kospot.infrastructure.websocket.handler;

import com.kospot.application.lobby.http.usecase.LeaveGlobalLobbyUseCase;
import com.kospot.application.multiplayer.gameroom.http.usecase.LeaveGameRoomUseCase;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.redis.service.SessionContextRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SessionContextRedisService sessionContextRedisService;

    //usecase
    private final LeaveGlobalLobbyUseCase leaveGlobalLobbyUseCase;
    private final LeaveGameRoomUseCase leaveGameRoomUseCase;

    //adaptor
    private final MemberAdaptor memberAdaptor;

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
        String gameRoomId = sessionContextRedisService.getAttr(sessionId, "roomId", String.class);
        String memberId = sessionContextRedisService.getAttr(sessionId, "memberId", String.class);
        Member member = memberAdaptor.queryById(Long.parseLong(memberId));

        // 비즈니스 로직 처리
        List<Runnable> cleanUpTasks = Arrays.asList(
                () -> leaveGlobalLobbyUseCase.execute(headerAccessor)
        );
        if (gameRoomId != null) {
            cleanUpTasks.add(
                    () -> leaveGameRoomUseCase.execute(member, Long.parseLong(gameRoomId))
            );
        }
        cleanUpTasks.parallelStream().forEach(Runnable::run);

        // Redis에서 세션 정보 삭제
        sessionContextRedisService.removeAttr(sessionId, "roomId");
        sessionContextRedisService.removeAttr(sessionId, "memberId");

        log.info("WebSocket 연결 해제 - SessionId: {}", sessionId);
    }

    private void safeCleanup(Runnable cleanup, String errorMessage) {
        try {
            cleanup.run();
        } catch (Exception e) {
            log.debug(errorMessage, e);
        }
    }

}
