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

    private static final String SESSION_KEY_PATTERN = "websocket:session:%s";
    private static final String SUBS_KEY_PATTERN = "websocket:subs:%s";
    private static final Duration SESSION_TTL = Duration.ofHours(2);

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
            removeAllSubscriptions(sessionId);
        } catch (Exception e) {
            log.error("Failed to cleanup session - SessionId: {}", sessionId, e);
        }
    }
    public void saveSubscription(String sessionId, String subscriptionId, String destination) {
        try {
            String subsKey = String.format(SUBS_KEY_PATTERN, sessionId);
            redisTemplate.opsForHash().put(subsKey, subscriptionId, destination);
            redisTemplate.expire(subsKey, SESSION_TTL);
        } catch (Exception e) {
            log.error("Failed to save subscription - SessionId: {}, SubscriptionId: {}", sessionId, subscriptionId, e);
        }
    }

    public String getSubscription(String sessionId, String subscriptionId) {
        try {
            String subsKey = String.format(SUBS_KEY_PATTERN, sessionId);
            Object value = redisTemplate.opsForHash().get(subsKey, subscriptionId);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("Failed to get subscription - SessionId: {}, SubscriptionId: {}", sessionId, subscriptionId, e);
            return null;
        }
    }

    public void removeSubscription(String sessionId, String subscriptionId) {
        try {
            String subsKey = String.format(SUBS_KEY_PATTERN, sessionId);
            redisTemplate.opsForHash().delete(subsKey, subscriptionId);
            Long size = redisTemplate.opsForHash().size(subsKey);
            if (size == null || size == 0) {
                redisTemplate.delete(subsKey);
            }
        } catch (Exception e) {
            log.error("Failed to remove subscription - SessionId: {}, SubscriptionId: {}", sessionId, subscriptionId, e);
        }
    }

    public void removeAllSubscriptions(String sessionId) {
        try {
            String subsKey = String.format(SUBS_KEY_PATTERN, sessionId);
            redisTemplate.delete(subsKey);
        } catch (Exception e) {
            log.error("Failed to remove all subscriptions - SessionId: {}", sessionId, e);
        }
    }
}
