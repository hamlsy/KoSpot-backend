package com.kospot.friend.infrastructure.redis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.common.redis.common.constants.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendCacheRedisService {

    private static final Duration FRIEND_LIST_TTL = Duration.ofMinutes(5);
    private static final Duration INCOMING_LIST_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> Optional<List<T>> getFriendList(Long memberId, TypeReference<List<T>> typeReference) {
        String key = friendListKey(memberId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, typeReference));
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse friend list cache. key={}", key, e);
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    public <T> void setFriendList(Long memberId, List<T> data) {
        String key = friendListKey(memberId);
        writeJsonWithTtl(key, data, FRIEND_LIST_TTL);
    }

    public <T> Optional<List<T>> getIncomingRequests(Long memberId, TypeReference<List<T>> typeReference) {
        String key = incomingKey(memberId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, typeReference));
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse incoming friend request cache. key={}", key, e);
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    public <T> void setIncomingRequests(Long memberId, List<T> data) {
        String key = incomingKey(memberId);
        writeJsonWithTtl(key, data, INCOMING_LIST_TTL);
    }

    public void evictFriendList(Long memberId) {
        redisTemplate.delete(friendListKey(memberId));
    }

    public void evictIncomingRequests(Long memberId) {
        redisTemplate.delete(incomingKey(memberId));
    }

    private String friendListKey(Long memberId) {
        return String.format(RedisKeyConstants.FRIEND_LIST_KEY_PATTERN, memberId);
    }

    private String incomingKey(Long memberId) {
        return String.format(RedisKeyConstants.FRIEND_INCOMING_KEY_PATTERN, memberId);
    }

    private <T> void writeJsonWithTtl(String key, T data, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to write friend cache. key={}", key, e);
        }
    }
}
