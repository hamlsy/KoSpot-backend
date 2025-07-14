package com.kospot.infrastructure.websocket.interceptor;

import com.kospot.infrastructure.exception.object.domain.WebSocketHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.security.service.TokenService;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.constants.WebSocketChannelConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.kospot.infrastructure.websocket.constants.WebSocketChannelConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatChannelInterceptor implements ChannelInterceptor {

    private final RedisTemplate<String, String> redisTemplate;
    private final TokenService tokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message); // stomp

        switch (accessor.getCommand()) {
            case CONNECT -> handleConnect(accessor);
            case SEND -> handleSend(accessor);
            case SUBSCRIBE -> handleSubscribe(accessor);
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) { // websocket 연결시
            handleConnect(accessor);
        } else if (StompCommand.SEND.equals(accessor.getCommand())) { // 메시지 보낼때
            handleSend(accessor);
        }
        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        WebSocketMemberPrincipal principal = createPrincipalFromToken(token);
        accessor.setUser(principal);
        accessor.getSessionAttributes().put("user", principal);
    }

    public void handleSend(StompHeaderAccessor accessor) {
        // 메시지 전송 시 처리 로직 (예: 메시지 검증, 로깅 등)
        // 현재는 rate limiting만 처리 중
        WebSocketMemberPrincipal principal = (WebSocketMemberPrincipal) accessor.getSessionAttributes().get("user");
        Long memberId = principal.getMemberId();
        log.info("Chat message from memberId: {}", memberId);
        if (isRateLimit(memberId)) {
            throw new WebSocketHandler(ErrorStatus.CHAT_RATE_LIMIT_EXCEEDED);
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        WebSocketMemberPrincipal principal = getPrincipal(accessor);
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new WebSocketHandler(ErrorStatus.INVALID_DESTINATION);
        }
        validateSubscriptionAccess(principal, destination);
        processSubscription(principal, destination, accessor.getSessionId());
    }

    private void processSubscription(WebSocketMemberPrincipal principal, String destination, String sessionId) {
        if (destination.startsWith(PREFIX_GAME_ROOM)) {

        }
    }

    private void validateSubscriptionAccess(WebSocketMemberPrincipal principal, String destination) {
        if(destination.startsWith(PREFIX_GAME_ROOM)) {

        }else if(destination.startsWith(PREFIX_CHAT)) {

        } else {
            throw new WebSocketHandler(ErrorStatus.INVALID_DESTINATION);
        }
    }

    private WebSocketMemberPrincipal getPrincipal(StompHeaderAccessor accessor) {
        WebSocketMemberPrincipal principal = (WebSocketMemberPrincipal) accessor.getSessionAttributes().get("user");
        if(principal == null) {
            throw new WebSocketHandler(ErrorStatus.UNAUTHORIZED); //todo implement custom exception
        }
        return principal;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String nativeToken = accessor.getFirstNativeHeader("Authorization");
        String token = removeBearerHeader(nativeToken);
        tokenService.validateToken(token);
        return token;
    }

    private WebSocketMemberPrincipal createPrincipalFromToken(String token) {
        Long memberId = tokenService.getMemberIdFromToken(token);
        String nickname = tokenService.getNicknameFromToken(token);
        String email = tokenService.getEmailFromToken(token);
        String role = tokenService.getRoleFromToken(token);
        return new WebSocketMemberPrincipal(memberId, nickname, email, role);
    }

    // 채팅 제한
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

    //remove bearer header method
    private String removeBearerHeader(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

}


