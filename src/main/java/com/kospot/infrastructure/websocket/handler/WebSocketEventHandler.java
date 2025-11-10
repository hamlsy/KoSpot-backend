package com.kospot.infrastructure.websocket.handler;

import com.kospot.application.lobby.http.usecase.LeaveGlobalLobbyUseCase;
import com.kospot.application.multi.room.http.usecase.LeaveGameRoomUseCase;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.object.domain.WebSocketHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.redis.common.service.SessionContextRedisService;
import com.kospot.infrastructure.redis.domain.multi.room.adaptor.GameRoomRedisAdaptor;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.context.PendingLeaveContext;
import com.kospot.infrastructure.websocket.session.service.WebSocketSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    //session
    private final SessionContextRedisService sessionContextRedisService;
    private final WebSocketSessionService webSocketSessionService;

    //usecase
    private final LeaveGlobalLobbyUseCase leaveGlobalLobbyUseCase;
    private final LeaveGameRoomUseCase leaveGameRoomUseCase;

    //adaptor
    private final MemberAdaptor memberAdaptor;
    private final GameRoomRedisAdaptor gameRoomRedisAdaptor;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        if (sessionId != null) {
            sessionContextRedisService.setAttr(sessionId, "connectedAt", System.currentTimeMillis());
            log.info("WebSocket 연결 성공 - SessionId: {}", sessionId);
        }
    }

//    @EventListener
//    public void handleWebSocketUnSubscribeListener(SessionUnsubscribeEvent event) {
//        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
//        WebSocketMemberPrincipal principal = getPrincipal(accessor);
//        String sessionId = accessor.getSessionId();
//        String subscriptionId = accessor.getSubscriptionId();
//        String destination = webSocketSessionService.getSubscription(sessionId, subscriptionId);
//
//
//    }


    // 클라이언트 연결 해제 시 처리
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) {
            return;
        }

        Long memberId = sessionContextRedisService.getAttr(sessionId, "memberId", Long.class);
        if (memberId == null) {
            cleanupSessionContext(sessionId);
            log.info("WebSocket 연결 해제 - SessionId: {} (memberId 없음)", sessionId);
            return;
        }

        Member member = memberAdaptor.queryById(memberId);
        Long gameRoomId = member.getGameRoomId();

        String headerReason = headerAccessor.getFirstNativeHeader("reason");
        String storedReason = sessionContextRedisService.getAttr(sessionId, "disconnectReason", String.class);
        String reason = Optional.ofNullable(headerReason)
                .filter(r -> !r.isBlank())
                .orElse(Optional.ofNullable(storedReason).orElse("unknown"));

        PendingLeaveContext pending = sessionContextRedisService.getAttr(sessionId, "pendingRoomLeave", PendingLeaveContext.class);
        String sessionVersion = sessionContextRedisService.getAttr(sessionId, "sessionVersion", String.class);

        boolean skipRoomLeave = shouldSkipRoomLeave(reason, pending, sessionVersion);

        try {
            leaveGlobalLobbyUseCase.execute(headerAccessor);
        } catch (Exception e) {
            log.warn("Failed to leave global lobby - SessionId: {}", sessionId, e);
        }

        if (!skipRoomLeave && gameRoomId != null) {
            try {
                leaveGameRoomUseCase.execute(member, gameRoomId);
                log.info("Member left game room - MemberId: {}, RoomId: {}", memberId, gameRoomId);
            } catch (Exception e) {
                log.warn("Failed to leave game room - MemberId: {}, RoomId: {}", memberId, gameRoomId, e);
            }
        } else if (skipRoomLeave) {
            log.info("Skip room leave due to graceful navigation - MemberId: {}, RoomId: {}", memberId, gameRoomId);
        }

        webSocketSessionService.cleanupSession(sessionId);
        cleanupSessionContext(sessionId);

        log.info("WebSocket 연결 해제 - SessionId: {}, Reason: {}", sessionId, reason);
    }


    private boolean shouldSkipRoomLeave(String reason,
                                        PendingLeaveContext pending,
                                        String sessionVersion) {
        if (pending == null) {
            return false;
        }
        boolean versionMatches = pending.getSessionVersion() == null
                || Objects.equals(pending.getSessionVersion(), sessionVersion);
        if (!versionMatches) {
            return false;
        }
        boolean isNavigation = "navigate-room".equalsIgnoreCase(reason)
                || "navigate-room".equalsIgnoreCase(pending.getReason());
        boolean stillValid = pending.getExpiresAt() > System.currentTimeMillis();
        return isNavigation && stillValid;
    }

    private void cleanupSessionContext(String sessionId) {
        sessionContextRedisService.removeAttr(sessionId, "pendingRoomLeave");
        sessionContextRedisService.removeAttr(sessionId, "disconnectReason");
        sessionContextRedisService.removeAttr(sessionId, "sessionVersion");
        sessionContextRedisService.removeAttr(sessionId, "memberId");
        sessionContextRedisService.removeAttr(sessionId, "connectedAt");
    }

    private void safeCleanup(Runnable cleanup, String errorMessage) {
        try {
            cleanup.run();
        } catch (Exception e) {
            log.debug(errorMessage, e);
        }
    }

    private WebSocketMemberPrincipal getPrincipal(StompHeaderAccessor accessor) {
        var sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new WebSocketHandler(ErrorStatus._UNAUTHORIZED);
        }

        WebSocketMemberPrincipal principal = (WebSocketMemberPrincipal) sessionAttributes.get("user");
        if (principal == null) {
            throw new WebSocketHandler(ErrorStatus._UNAUTHORIZED);
        }
        return principal;
    }

}
