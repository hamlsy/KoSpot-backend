package com.kospot.infrastructure.redis.domain.chat.transientstore;

import com.kospot.infrastructure.redis.common.constants.RedisKeyConstants;
import com.kospot.infrastructure.redis.domain.chat.transientstore.config.TransientChatProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TransientChatRedisStore {

    private static final Logger log = LoggerFactory.getLogger(TransientChatRedisStore.class);

    public static final String GLOBAL_LOBBY_CHAT_KEY = RedisKeyConstants.TRANSIENT_GLOBAL_LOBBY_CHAT_KEY;

    private final RedisTemplate<String, Object> redisTemplate;
    private final TransientChatProperties properties;

    public TransientChatRedisStore(RedisTemplate<String, Object> redisTemplate, TransientChatProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void store(String key, Object payload, long createdAtMillis) {
        if (!properties.isEnabled()) {
            return;
        }

        Duration ttl = resolveTtl();
        double evictionBoundary = createdAtMillis - ttl.toMillis();

        try {
            redisTemplate.opsForZSet().add(key, payload, createdAtMillis);
            redisTemplate.opsForZSet().removeRangeByScore(key, Double.NEGATIVE_INFINITY, evictionBoundary);
            redisTemplate.expire(key, ttl);
        } catch (Exception e) {
            log.error("Failed to store transient chat message. key={}", key, e);
        }
    }

    public static String gameRoomChatKey(Long roomId) {
        return String.format(RedisKeyConstants.TRANSIENT_GAME_ROOM_CHAT_KEY_PATTERN, String.valueOf(roomId));
    }

    public static String globalGameChatKey(Long roomId) {
        return String.format(RedisKeyConstants.TRANSIENT_GLOBAL_GAME_CHAT_KEY_PATTERN, String.valueOf(roomId));
    }

    private Duration resolveTtl() {
        long ttlDays = Math.max(1L, properties.getTtlDays());
        return Duration.ofDays(ttlDays);
    }
}
