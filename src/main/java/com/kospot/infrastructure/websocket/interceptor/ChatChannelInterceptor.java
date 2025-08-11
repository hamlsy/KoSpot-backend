package com.kospot.infrastructure.websocket.interceptor;

import com.kospot.infrastructure.exception.object.domain.WebSocketHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.security.service.TokenService;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.constants.WebSocketChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

import static com.kospot.infrastructure.websocket.constants.WebSocketChannelConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatChannelInterceptor implements ChannelInterceptor {

    private final RedisTemplate<String, String> redisTemplate;
    private final TokenService tokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
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
        
        // Enum 기반 구독 권한 검증
        validateBasicSubscriptionAccess(principal, destination);
        
        // 세션 정보 저장 (연결 해제 시 정리용)
        saveSessionInfo(accessor.getSessionId(), destination, principal);
        
        log.info("Subscription registered - MemberId: {}, Destination: {}, SessionId: {}", 
                principal.getMemberId(), destination, accessor.getSessionId());
    }

    /**
     * 연결 해제 시 세션 정리
     */
    private void handleDisconnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            cleanupSession(sessionId);
            log.info("WebSocket disconnected - SessionId: {}", sessionId);
        }
    }

    /**
     * Enum 기반 구독 권한 검증 (확장 가능한 방식)
     */
    private void validateBasicSubscriptionAccess(WebSocketMemberPrincipal principal, String destination) {
        Optional<WebSocketChannelType> channelType = WebSocketChannelType.fromDestination(destination);
        
        if (channelType.isPresent()) {
            WebSocketChannelType type = channelType.get();
            
            // 채널 타입별 세부 접근 권한 검증
            if (type.canAccess(principal.getMemberId(), destination)) {
                log.debug("Valid subscription - MemberId: {}, Destination: {}, ChannelType: {} [{}]", 
                         principal.getMemberId(), destination, type.getDisplayName(), type.getAccessLevel());
                return;
            } else {
                log.warn("Access denied - MemberId: {}, Destination: {}, ChannelType: {}, AccessLevel: {}", 
                         principal.getMemberId(), destination, type.getDisplayName(), type.getAccessLevel());
                throw new WebSocketHandler(ErrorStatus._FORBIDDEN);
            }
        }
        
        log.warn("Invalid destination - MemberId: {}, Destination: {}, AllowedPrefixes: {}", 
                 principal.getMemberId(), destination, WebSocketChannelType.getAllowedPrefixes());
        throw new WebSocketHandler(ErrorStatus.INVALID_DESTINATION);
    }

    /**
     * 세션 정보 저장 (Redis)
     */
    private void saveSessionInfo(String sessionId, String destination, WebSocketMemberPrincipal principal) {
        try {
            String sessionKey = "websocket:session:" + sessionId;
            String sessionData = String.format(
                "{\"memberId\":%d,\"destination\":\"%s\",\"timestamp\":%d}",
                principal.getMemberId(), destination, System.currentTimeMillis()
            );
            redisTemplate.opsForValue().set(sessionKey, sessionData, Duration.ofHours(2));
        } catch (Exception e) {
            log.error("Failed to save session info - SessionId: {}", sessionId, e);
        }
    }

    /**
     * 세션 정보 정리
     */
    private void cleanupSession(String sessionId) {
        try {
            String sessionKey = "websocket:session:" + sessionId;
            redisTemplate.delete(sessionKey);
        } catch (Exception e) {
            log.error("Failed to cleanup session - SessionId: {}", sessionId, e);
        }
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


