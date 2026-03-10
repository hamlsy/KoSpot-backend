package com.kospot.common.websocket.handler;

import com.kospot.multi.lobby.application.usecase.JoinGlobalLobbyUseCase;
import com.kospot.multi.lobby.application.usecase.LeaveGlobalLobbyUseCase;
import com.kospot.common.websocket.connection.service.WebSocketConnectionStateOrchestrator;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.redis.common.service.SessionContextRedisService;

import com.kospot.common.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.friend.infrastructure.websocket.constants.FriendChatChannelConstants;
import com.kospot.friend.infrastructure.websocket.service.FriendChatSubscriptionCacheService;
import com.kospot.common.websocket.session.service.WebSocketSessionService;
import com.kospot.multi.room.infrastructure.redis.adaptor.GameRoomRedisAdaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import static com.kospot.multi.lobby.infrastructure.websocket.constants.LobbyChannelConstants.PREFIX_CHAT;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    //session
    private final SessionContextRedisService sessionContextRedisService;
    private final WebSocketSessionService webSocketSessionService;
    private final FriendChatSubscriptionCacheService friendChatSubscriptionCacheService;

    //usecase
    private final LeaveGlobalLobbyUseCase leaveGlobalLobbyUseCase;
    private final JoinGlobalLobbyUseCase joinGlobalLobbyUseCase;
    private final WebSocketConnectionStateOrchestrator webSocketConnectionStateOrchestrator;

    //adaptor
    private final MemberAdaptor memberAdaptor;
    private final GameRoomRedisAdaptor gameRoomRedisAdaptor;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        if (sessionId != null) {
            log.info("WebSocket 연결 성공 - SessionId: {}", sessionId);
        }
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String dest = headerAccessor.getDestination();
//        if ("/topic/chat/lobby".equals(dest)) {
//            joinGlobalLobbyUseCase.execute(headerAccessor); // join 처리
//        }
    }

    @EventListener
    public void handleWebSocketUnSubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();
        if (sessionId == null || subscriptionId == null) {
            return;
        }

        WebSocketMemberPrincipal principal = getPrincipalOrNull(accessor);
        String destination = webSocketSessionService.getSubscription(sessionId, subscriptionId);

        if (destination != null && destination.startsWith(FriendChatChannelConstants.PREFIX_FRIEND_CHAT_ROOM)) {
            Long roomId = FriendChatChannelConstants.extractRoomIdFromDestination(destination);
            if (roomId != null) {
                friendChatSubscriptionCacheService.removeRoom(sessionId, roomId);
            }
        }

        if (destination != null && destination.startsWith(PREFIX_CHAT)) {
            leaveGlobalLobbyUseCase.execute(accessor);
        }

//        if (destination != null && destination.startsWith(PREFIX_GAME_ROOM)) {
//            try {
//                Member member = memberAdaptor.queryById(principal.getMemberId());
//                Long gameRoomId = member.getGameRoomId();
//                if (gameRoomId != null) {
//                    leaveGameRoomUseCase.execute(member, gameRoomId);
//                }
//                log.info("Member left game room on unsubscribe - MemberId: {}", principal.getMemberId());
//            } catch (Exception e) {
//                log.warn("Failed to leave game room on unsubscribe - MemberId: {}", principal.getMemberId(), e);
//            }
//        }

        webSocketSessionService.removeSubscription(sessionId, subscriptionId);
        if (principal != null) {
            log.info("Unsubscribed - MemberId:{}, Destination:{}, SessionId:{}, SubId:{}",
                    principal.getMemberId(), destination, sessionId, subscriptionId);
            return;
        }
        log.info("Unsubscribed - Destination:{}, SessionId:{}, SubId:{}",
                destination, sessionId, subscriptionId);

    }


    // 클라이언트 연결 해제 시 처리
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) {
            return;
        }

        String reason = headerAccessor.getFirstNativeHeader("reason");
        if (reason == null || reason.isBlank()) {
            reason = "unknown";
        }

        Long memberId = sessionContextRedisService.getAttr(sessionId, "memberId", Long.class);
        if (memberId == null) {
            webSocketSessionService.cleanupSession(sessionId);
            friendChatSubscriptionCacheService.removeSession(sessionId);
            sessionContextRedisService.removeAllAttr(sessionId);
            log.info("WebSocket 연결 해제 - SessionId: {} (memberId 없음), Reason: {}", sessionId, reason);
            return;
        }

        Member member = memberAdaptor.queryById(memberId);
        Long gameRoomId = member.getGameRoomId();

        boolean skipRoomLeave = shouldSkipRoomLeave(reason);

        try {
            leaveGlobalLobbyUseCase.execute(headerAccessor);
        } catch (Exception e) {
            log.warn("Failed to leave global lobby - SessionId: {}", sessionId, e);
        }

        if (skipRoomLeave) {
            log.info("Skip room leave due to graceful navigation - MemberId: {}, RoomId: {}", memberId, gameRoomId);
        }

        webSocketConnectionStateOrchestrator.handleDisconnect(memberId, sessionId, gameRoomId, reason, skipRoomLeave);

        log.info("WebSocket 연결 해제 - SessionId: {}, Reason: {}", sessionId, reason);
    }


    private boolean shouldSkipRoomLeave(String reason) {
        return "navigate-room".equalsIgnoreCase(reason);
    }
    private WebSocketMemberPrincipal getPrincipalOrNull(StompHeaderAccessor accessor) {
        var sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return null;
        }

        WebSocketMemberPrincipal principal = (WebSocketMemberPrincipal) sessionAttributes.get("user");
        if (principal == null) {
            return null;
        }
        return principal;
    }

}
