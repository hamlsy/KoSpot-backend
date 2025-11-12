package com.kospot.infrastructure.redis.domain.member.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class MemberProfileRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 프로필 저장 (nickname, markerImageUrl)
     */
    public void saveProfile(String key, String nickname, String markerImageUrl, long expireHours) {
        redisTemplate.opsForHash().put(key, "nickname", nickname);
        redisTemplate.opsForHash().put(key, "markerImageUrl", markerImageUrl);
        redisTemplate.expire(key, expireHours, TimeUnit.HOURS);
    }

    /**
     * 전체 프로필 조회
     */
    public Map<Object, Object> findProfile(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 단일 필드 조회 (예: nickname만)
     */
    public Object findField(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 단일 필드 저장/수정
     */
    public void saveField(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 프로필 삭제
     */
    public void deleteProfile(String key) {
        redisTemplate.delete(key);
    }

}
