package com.kospot.common.websocket.handler;

import com.kospot.multi.lobby.application.usecase.JoinGlobalLobbyUseCase;
import com.kospot.multi.lobby.application.usecase.LeaveGlobalLobbyUseCase;
import com.kospot.multi.room.application.usecase.LeaveGameRoomUseCase;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.exception.object.domain.WebSocketHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
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
    private final LeaveGameRoomUseCase leaveGameRoomUseCase;
    private final JoinGlobalLobbyUseCase joinGlobalLobbyUseCase;

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

    @EventListener
    public void handleWebSocketSubscribeLisnter(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String dest = headerAccessor.getDestination();
        if ("/topic/chat/lobby".equals(dest)) {
            joinGlobalLobbyUseCase.execute(headerAccessor); // join 처리
        }
    }

    @EventListener
    public void handleWebSocketUnSubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        WebSocketMemberPrincipal principal = getPrincipal(accessor);
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();
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
        log.info("Unsubscribed - MemberId:{}, Destination:{}, SessionId:{}, SubId:{}",
                principal.getMemberId(), destination, sessionId, subscriptionId);

    }


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
            sessionContextRedisService.removeAllAttr(sessionId);
            log.info("WebSocket 연결 해제 - SessionId: {} (memberId 없음)", sessionId);
            return;
        }

        Member member = memberAdaptor.queryById(memberId);
        Long gameRoomId = member.getGameRoomId();

        String reason = headerAccessor.getFirstNativeHeader("reason");
        if (reason == null || reason.isBlank()) {
            reason = "unknown";
        }

        boolean skipRoomLeave = shouldSkipRoomLeave(reason);

        try {
            leaveGlobalLobbyUseCase.execute(headerAccessor);
        } catch (Exception e) {
            log.warn("Failed to leave global lobby - SessionId: {}", sessionId, e);
        }

        if (!skipRoomLeave && gameRoomId != null) {
            try {
                leaveGameRoomUseCase.execute(member.getId(), gameRoomId);
                log.info("Member left game room - MemberId: {}, RoomId: {}", memberId, gameRoomId);
            } catch (Exception e) {
                log.warn("Failed to leave game room - MemberId: {}, RoomId: {}", memberId, gameRoomId, e);
            }
        } else if (skipRoomLeave) {
            log.info("Skip room leave due to graceful navigation - MemberId: {}, RoomId: {}", memberId, gameRoomId);
        }

        webSocketSessionService.cleanupSession(sessionId);
        friendChatSubscriptionCacheService.removeSession(sessionId);
        sessionContextRedisService.removeAllAttr(sessionId);

        log.info("WebSocket 연결 해제 - SessionId: {}, Reason: {}", sessionId, reason);
    }


    private boolean shouldSkipRoomLeave(String reason) {
        return "navigate-room".equalsIgnoreCase(reason);
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
