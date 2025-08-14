package com.kospot.infrastructure.websocket.session.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveSessionInfo(String sessionId, String destination, WebSocketMemberPrincipal principal) {
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

    public void cleanupSession(String sessionId) {
        try {
            String sessionKey = "websocket:session:" + sessionId;
            redisTemplate.delete(sessionKey);
        } catch (Exception e) {
            log.error("Failed to cleanup session - SessionId: {}", sessionId, e);
        }
    }

}
