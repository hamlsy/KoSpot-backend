package com.kospot.infrastructure.websocket.interceptor;

import com.kospot.application.lobby.http.usecase.LeaveGlobalLobbyUseCase;
import com.kospot.application.multi.room.http.usecase.LeaveGameRoomUseCase;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.infrastructure.exception.object.domain.WebSocketHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.redis.common.service.SessionContextRedisService;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.infrastructure.security.service.TokenService;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.context.PendingLeaveContext;
import com.kospot.infrastructure.websocket.session.service.WebSocketSessionService;
import com.kospot.infrastructure.websocket.subscription.SubscriptionValidationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.UUID;

import static com.kospot.infrastructure.websocket.domain.multi.lobby.constants.LobbyChannelConstants.PREFIX_CHAT;
import static com.kospot.infrastructure.websocket.domain.multi.room.constants.GameRoomChannelConstants.PREFIX_GAME_ROOM;
import static com.kospot.infrastructure.websocket.domain.notification.constants.NotificationChannelConstants.PERSONAL_NOTIFICATION_SUBSCRIBE_CHANNEL;
import static com.kospot.infrastructure.websocket.domain.notification.constants.NotificationChannelConstants.PREFIX_NOTIFICATION;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final RedisTemplate<String, String> redisTemplate;
    private final TokenService tokenService;
    private final SubscriptionValidationManager subscriptionValidationManager;

    // session service
    private final WebSocketSessionService webSocketSessionService;
    private final SessionContextRedisService sessionContextRedisService;

    private static final long PENDING_LEAVE_GRACE_MILLIS = 4000L;
    private static final int RATE_LIMIT = 40; // 1분에 허용되는 메시지 수
    private static final String RATE_LIMIT_KEY = "rate_limit:chat:";

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (accessor.getCommand() != null) {
            switch (accessor.getCommand()) {
                case CONNECT -> handleConnect(accessor);
                case SEND -> handleSend(accessor);
                case SUBSCRIBE -> handleSubscribe(accessor);
                case DISCONNECT -> handleDisconnect(accessor);
                case UNSUBSCRIBE -> handleUnsubscribe(accessor);
                default -> {
                    // 기타 명령어들은 무시 (ACK, NACK, RECEIPT, ERROR 등)
                }
            }
        }

        return message;
    }

    /**
     * WebSocket 연결 시 인증 처리
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractTokenOrNull(accessor);

        WebSocketMemberPrincipal principal;
        if (token != null) {
            principal = createPrincipalFromToken(token);
        } else {
            // 부하테스트용 익명 Principal (프로덕션에서는 실제 인증 필요)
            principal = new WebSocketMemberPrincipal(-1L, "load-test", "test@loadtest.com", "TEST");
            log.debug("Anonymous WebSocket connection for load testing");
        }

        accessor.setUser(principal);

        String sessionId = accessor.getSessionId();
        String sessionVersion = UUID.randomUUID().toString();

        var sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put("user", principal);
            sessionAttributes.put("sessionVersion", sessionVersion);
        }
        if (sessionId != null && principal.getMemberId() > 0) {
            // 익명 연결(-1)은 Redis에 저장하지 않음
            sessionContextRedisService.setAttr(sessionId, "memberId", principal.getMemberId());
            sessionContextRedisService.setAttr(sessionId, "sessionVersion", sessionVersion);
            sessionContextRedisService.setAttr(sessionId, "connectedAt", System.currentTimeMillis());
        }
        log.info("WebSocket connected - MemberId: {}, SessionId: {}",
                principal.getMemberId(), accessor.getSessionId());
    }

    /**
     * 메시지 전송 시 Rate Limiting 처리
     */
    private void handleSend(StompHeaderAccessor accessor) {
        WebSocketMemberPrincipal principal = getPrincipal(accessor);
        Long memberId = principal.getMemberId();

        if (isRateLimit(memberId)) {
            log.warn("Rate limit exceeded - MemberId: {}", memberId);
            throw new WebSocketHandler(ErrorStatus.CHAT_RATE_LIMIT_EXCEEDED);
        }

        log.debug("Message sent - MemberId: {}, Destination: {}",
                memberId, accessor.getDestination());
    }

    /**
     * 구독 시 기본 권한 검증 및 세션 정보 저장
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        WebSocketMemberPrincipal principal = getPrincipal(accessor);
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();

        if (destination == null) {
            throw new WebSocketHandler(ErrorStatus.INVALID_DESTINATION);
        }

        // 알림 채널만 우선 적용 (기존 채널들 영향 최소화)
        if (needsSubscriptionValidation(destination)) {
            validateSubscriptionAccess(principal, destination);
        }

        // 세션 정보 저장 (연결 해제 시 정리용)
        webSocketSessionService.saveSessionInfo(accessor.getSessionId(), destination, principal);
        if (subscriptionId != null) {
            webSocketSessionService.saveSubscription(sessionId, subscriptionId, destination);
        }

        log.info("Subscription registered - MemberId: {}, Destination: {}, SessionId: {}",
                principal.getMemberId(), destination, sessionId);
    }

    private boolean needsSubscriptionValidation(String destination) {
        if (destination == null) {
            return false;
        }

        if (destination.startsWith(PREFIX_NOTIFICATION)) {
            return true;
        }

        // 개인 알림 채널: /user/queue/notification
        return PERSONAL_NOTIFICATION_SUBSCRIBE_CHANNEL.equals(destination);
    }

    /**
     * 연결 해제 시 세션 정리
     */
    // todo refactoring
    private void handleDisconnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return;
        }
    }

    private void handleUnsubscribe(StompHeaderAccessor accessor) {
    }

    /**
     * 확장 가능한 구독 권한 검증 (전략 패턴 적용)
     */
    private void validateSubscriptionAccess(WebSocketMemberPrincipal principal, String destination) {
        boolean canSubscribe = subscriptionValidationManager.validateSubscription(principal, destination);

        if (!canSubscribe) {
            log.warn("Subscription access denied - MemberId: {}, Destination: {}, SupportedPrefixes: {}",
                    principal.getMemberId(), destination, subscriptionValidationManager.getSupportedPrefixes());
            throw new WebSocketHandler(ErrorStatus._FORBIDDEN);
        }

        log.debug("Subscription access granted - MemberId: {}, Destination: {}, ValidatorStats: {}",
                principal.getMemberId(), destination, subscriptionValidationManager.getValidationStatistics());
    }

    /**
     * 사용자 Principal 추출
     */
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

    /**
     * JWT 토큰 추출 (예외 발생)
     */
    private String extractToken(StompHeaderAccessor accessor) {
        String nativeToken = accessor.getFirstNativeHeader("Authorization");
        String token = removeBearerHeader(nativeToken);
        tokenService.validateToken(token);
        return token;
    }

    /**
     * JWT 토큰 추출 (null 반환, 부하테스트용)
     */
    private String extractTokenOrNull(StompHeaderAccessor accessor) {
        try {
            String nativeToken = accessor.getFirstNativeHeader("Authorization");
            if (nativeToken == null || nativeToken.isBlank()) {
                return null;
            }
            String token = removeBearerHeader(nativeToken);
            tokenService.validateToken(token);
            return token;
        } catch (Exception e) {
            log.debug("Token extraction failed, allowing anonymous connection: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JWT 토큰으로부터 Principal 생성
     */
    private WebSocketMemberPrincipal createPrincipalFromToken(String token) {
        Long memberId = tokenService.getMemberIdFromToken(token);
        String nickname = tokenService.getNicknameFromToken(token);
        String email = tokenService.getEmailFromToken(token);
        String role = tokenService.getRoleFromToken(token);
        return new WebSocketMemberPrincipal(memberId, nickname, email, role);
    }

    /**
     * 채팅 Rate Limiting
     */
    private boolean isRateLimit(Long memberId) {
        String key = RATE_LIMIT_KEY + memberId;
        String count = redisTemplate.opsForValue().get(key);

        if (count == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(1));
            return false;
        }

        int currentCount = Integer.parseInt(count);
        if (currentCount >= RATE_LIMIT) {
            return true;
        }

        redisTemplate.opsForValue().increment(key);
        return false;
    }

    /**
     * Bearer 헤더 제거
     */
    private String removeBearerHeader(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

}
