package com.kospot.notice.infrastructure.redis.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.common.redis.common.constants.RedisKeyConstants;
import com.kospot.notice.infrastructure.redis.vo.NoticeCacheData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 최근 공지사항 캐시 Repository
 * Redis String 자료구조에 JSON 형태로 공지사항 목록 저장
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class NoticeCacheRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration CACHE_TTL = Duration.ofHours(24);

    /**
     * 최근 공지사항 목록을 캐시에 저장
     */
    public void saveAll(List<NoticeCacheData> notices) {
        try {
            String json = objectMapper.writeValueAsString(notices);
            redisTemplate.opsForValue().set(
                    RedisKeyConstants.RECENT_NOTICES_KEY,
                    json,
                    CACHE_TTL);
            log.debug("Recent notices cached: {} items", notices.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notices for cache", e);
        }
    }

    /**
     * 캐시에서 최근 공지사항 목록 조회
     */
    public Optional<List<NoticeCacheData>> findAll() {
        String json = redisTemplate.opsForValue().get(RedisKeyConstants.RECENT_NOTICES_KEY);

        if (json == null) {
            return Optional.empty();
        }

        try {
            List<NoticeCacheData> notices = objectMapper.readValue(
                    json,
                    new TypeReference<List<NoticeCacheData>>() {
                    });
            return Optional.of(notices);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize notices from cache", e);
            return Optional.empty();
        }
    }

    /**
     * 캐시 삭제 (무효화)
     */
    public void deleteAll() {
        redisTemplate.delete(RedisKeyConstants.RECENT_NOTICES_KEY);
        log.debug("Recent notices cache invalidated");
    }

    /**
     * 캐시 존재 여부 확인
     */
    public boolean exists() {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(RedisKeyConstants.RECENT_NOTICES_KEY));
    }
}
