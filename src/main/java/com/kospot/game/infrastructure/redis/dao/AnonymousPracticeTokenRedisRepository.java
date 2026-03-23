package com.kospot.game.infrastructure.redis.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class AnonymousPracticeTokenRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public void save(String key, String token, long expireHours) {
        redisTemplate.opsForValue().set(key, token, expireHours, TimeUnit.HOURS);
    }

    public String find(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
