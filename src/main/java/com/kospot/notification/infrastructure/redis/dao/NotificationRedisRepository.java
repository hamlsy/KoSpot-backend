package com.kospot.notification.infrastructure.redis.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.common.redis.common.constants.RedisKeyConstants;
import com.kospot.notification.infrastructure.redis.vo.NotificationRedisData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public class NotificationRedisRepository {

    private static final Logger log = LoggerFactory.getLogger(NotificationRedisRepository.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public NotificationRedisRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public long nextId() {
        Long value = redisTemplate.opsForValue().increment(RedisKeyConstants.NOTIFICATION_SEQ_KEY);
        return value != null ? value : System.currentTimeMillis();
    }

    public void saveItem(NotificationRedisData data, Duration ttl) {
        String key = itemKey(data.getNotificationId());
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize notification", e);
        }
    }

    public Optional<NotificationRedisData> findItem(Long notificationId) {
        String key = itemKey(notificationId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, NotificationRedisData.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize notification - NotificationId: {}", notificationId, e);
            return Optional.empty();
        }
    }

    public void deleteItem(Long notificationId) {
        redisTemplate.delete(itemKey(notificationId));
    }

    public void addToIndex(Long receiverMemberId, Long notificationId, long createdAtMillis, Duration ttl) {
        String indexKey = indexKey(receiverMemberId);
        redisTemplate.opsForZSet().add(indexKey, String.valueOf(notificationId), (double) createdAtMillis);
        redisTemplate.expire(indexKey, ttl);
    }

    public Set<String> getIndexedIds(Long receiverMemberId, long start, long end) {
        String indexKey = indexKey(receiverMemberId);
        Set<String> ids = redisTemplate.opsForZSet().reverseRange(indexKey, start, end);
        return ids;
    }

    public void removeFromIndex(Long receiverMemberId, Collection<String> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        String indexKey = indexKey(receiverMemberId);
        redisTemplate.opsForZSet().remove(indexKey, notificationIds.toArray());
    }

    public void addUnread(Long receiverMemberId, Long notificationId, Duration ttl) {
        String unreadKey = unreadKey(receiverMemberId);
        redisTemplate.opsForSet().add(unreadKey, String.valueOf(notificationId));
        redisTemplate.expire(unreadKey, ttl);
    }

    public long countUnread(Long receiverMemberId) {
        String unreadKey = unreadKey(receiverMemberId);
        Long value = redisTemplate.opsForSet().size(unreadKey);
        return value != null ? value : 0L;
    }

    public Set<String> getUnreadIds(Long receiverMemberId) {
        String unreadKey = unreadKey(receiverMemberId);
        return redisTemplate.opsForSet().members(unreadKey);
    }

    public void removeUnread(Long receiverMemberId, Collection<String> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        String unreadKey = unreadKey(receiverMemberId);
        redisTemplate.opsForSet().remove(unreadKey, notificationIds.toArray());
    }

    public void clearUnread(Long receiverMemberId) {
        redisTemplate.delete(unreadKey(receiverMemberId));
    }

    private String itemKey(Long notificationId) {
        return String.format(RedisKeyConstants.NOTIFICATION_ITEM_KEY_PATTERN, String.valueOf(notificationId));
    }

    private String indexKey(Long receiverMemberId) {
        return String.format(RedisKeyConstants.NOTIFICATION_USER_INDEX_KEY_PATTERN, String.valueOf(receiverMemberId));
    }

    private String unreadKey(Long receiverMemberId) {
        return String.format(RedisKeyConstants.NOTIFICATION_USER_UNREAD_KEY_PATTERN, String.valueOf(receiverMemberId));
    }
}
