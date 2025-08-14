package com.kospot.infrastructure.websocket.interceptor;

import com.kospot.infrastructure.exception.object.domain.WebSocketHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.security.service.TokenService;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
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

import static com.kospot.infrastructure.websocket.constants.WebSocketChannelConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final RedisTemplate<String, String> redisTemplate;
    private final TokenService tokenService;
    private final SubscriptionValidationManager subscriptionValidationManager;
    private final WebSocketSessionService webSocketSessionService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (accessor.getCommand() != null) {
            switch (accessor.getCommand()) {
                case CONNECT -> handleConnect(accessor);
                case SEND -> handleSend(accessor);
                case SUBSCRIBE -> handleSubscribe(accessor);
                case DISCONNECT -> handleDisconnect(accessor);
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
        String token = extractToken(accessor);
        WebSocketMemberPrincipal principal = createPrincipalFromToken(token);
        accessor.setUser(principal);
        
        var sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put("user", principal);
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
        
        if (destination == null) {
            throw new WebSocketHandler(ErrorStatus.INVALID_DESTINATION);
        }
        
        // 확장 가능한 구독 권한 검증
        validateSubscriptionAccess(principal, destination);
        
        // 세션 정보 저장 (연결 해제 시 정리용)
        webSocketSessionService.saveSessionInfo(accessor.getSessionId(), destination, principal);
        
        log.info("Subscription registered - MemberId: {}, Destination: {}, SessionId: {}", 
                principal.getMemberId(), destination, accessor.getSessionId());
    }

    /**
     * 연결 해제 시 세션 정리
     */
    private void handleDisconnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            webSocketSessionService.cleanupSession(sessionId);
            log.info("WebSocket disconnected - SessionId: {}", sessionId);
        }
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
     * JWT 토큰 추출
     */
    private String extractToken(StompHeaderAccessor accessor) {
        String nativeToken = accessor.getFirstNativeHeader("Authorization");
        String token = removeBearerHeader(nativeToken);
        tokenService.validateToken(token);
        return token;
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


