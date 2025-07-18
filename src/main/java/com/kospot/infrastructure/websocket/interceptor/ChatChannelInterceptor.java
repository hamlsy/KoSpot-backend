package com.kospot.infrastructure.websocket.interceptor;

import com.kospot.infrastructure.exception.object.domain.ChatHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.security.service.TokenService;
import com.kospot.infrastructure.websocket.auth.ChatMemberPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

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

        if (StompCommand.CONNECT.equals(accessor.getCommand())) { // websocket 연결시

            String nativeToken = accessor.getFirstNativeHeader("Authorization");
            String token = removeBearerHeader(nativeToken);
            tokenService.validateToken(token);

            Long memberId = tokenService.getMemberIdFromToken(token);
            String nickname = tokenService.getNicknameFromToken(token);
            String email = tokenService.getEmailFromToken(token);
            String role = tokenService.getRoleFromToken(token);
            ChatMemberPrincipal principal = new ChatMemberPrincipal(memberId, nickname, email, role);
            accessor.setUser(principal);
            accessor.getSessionAttributes().put("user", principal);
        } else if (StompCommand.SEND.equals(accessor.getCommand())) { // 메시지 보낼때
            // rate limiting 체크
            ChatMemberPrincipal principal = (ChatMemberPrincipal) accessor.getSessionAttributes().get("user");
            Long memberId = principal.getMemberId();
            log.info("Chat message from memberId: {}", memberId);
            if (isRateLimit(memberId)) {
                throw new ChatHandler(ErrorStatus.CHAT_RATE_LIMIT_EXCEEDED);
            }
        }
        return message;
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


