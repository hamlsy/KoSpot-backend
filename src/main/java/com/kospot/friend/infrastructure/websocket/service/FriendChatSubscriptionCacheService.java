package com.kospot.friend.infrastructure.websocket.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class FriendChatSubscriptionCacheService {

    private static final String KEY_PATTERN = "ws:friend:sub:%s";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate stringRedisTemplate;

    public FriendChatSubscriptionCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void allowRoom(String sessionId, Long roomId) {
        if (sessionId == null || roomId == null) {
            return;
        }

        String key = buildKey(sessionId);
        String room = String.valueOf(roomId);
        stringRedisTemplate.opsForSet().add(key, room);
        stringRedisTemplate.expire(key, TTL);
    }

    public boolean isAllowed(String sessionId, Long roomId) {
        if (sessionId == null || roomId == null) {
            return false;
        }

        String key = buildKey(sessionId);
        Boolean allowed = stringRedisTemplate.opsForSet().isMember(key, String.valueOf(roomId));
        if (Boolean.TRUE.equals(allowed)) {
            stringRedisTemplate.expire(key, TTL);
        }
        return Boolean.TRUE.equals(allowed);
    }

    public void removeRoom(String sessionId, Long roomId) {
        if (sessionId == null || roomId == null) {
            return;
        }
        stringRedisTemplate.opsForSet().remove(buildKey(sessionId), String.valueOf(roomId));
    }

    public void removeSession(String sessionId) {
        if (sessionId == null) {
            return;
        }
        stringRedisTemplate.delete(buildKey(sessionId));
    }

    private String buildKey(String sessionId) {
        return String.format(KEY_PATTERN, sessionId);
    }
}
