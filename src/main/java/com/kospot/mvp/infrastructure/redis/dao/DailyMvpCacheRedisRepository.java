package com.kospot.mvp.infrastructure.redis.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.mvp.presentation.response.DailyMvpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DailyMvpCacheRedisRepository {

    private static final String NONE_VALUE = "1";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<DailyMvpResponse.Daily> find(String cacheKey) {
        String json = redisTemplate.opsForValue().get(cacheKey);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, DailyMvpResponse.Daily.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse daily MVP cache. key={}", cacheKey, e);
            redisTemplate.delete(cacheKey);
            return Optional.empty();
        }
    }

    public void save(String cacheKey, DailyMvpResponse.Daily response, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to write daily MVP cache. key={}", cacheKey, e);
        }
    }

    public void saveNone(String noneKey, Duration ttl) {
        redisTemplate.opsForValue().set(noneKey, NONE_VALUE, ttl);
    }

    public boolean existsNone(String noneKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(noneKey));
    }

    public void delete(String cacheKey) {
        redisTemplate.delete(cacheKey);
    }

    public boolean acquireLock(String lockKey, Duration ttl) {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", ttl);
        return Boolean.TRUE.equals(locked);
    }

    public void releaseLock(String lockKey) {
        redisTemplate.delete(lockKey);
    }
}
