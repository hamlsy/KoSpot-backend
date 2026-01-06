package com.kospot.infrastructure.redis.domain.banner.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.infrastructure.redis.common.constants.RedisKeyConstants;
import com.kospot.infrastructure.redis.domain.banner.vo.BannerCacheData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 활성 배너 캐시 Repository
 * Redis String 자료구조에 JSON 형태로 배너 목록 저장
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BannerCacheRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration CACHE_TTL = Duration.ofHours(24);

    /**
     * 활성 배너 목록을 캐시에 저장
     */
    public void saveAll(List<BannerCacheData> banners) {
        try {
            String json = objectMapper.writeValueAsString(banners);
            redisTemplate.opsForValue().set(
                    RedisKeyConstants.ACTIVE_BANNERS_KEY,
                    json,
                    CACHE_TTL);
            log.debug("Active banners cached: {} items", banners.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize banners for cache", e);
        }
    }

    /**
     * 캐시에서 활성 배너 목록 조회
     */
    public Optional<List<BannerCacheData>> findAll() {
        String json = redisTemplate.opsForValue().get(RedisKeyConstants.ACTIVE_BANNERS_KEY);

        if (json == null) {
            return Optional.empty();
        }

        try {
            List<BannerCacheData> banners = objectMapper.readValue(
                    json,
                    new TypeReference<List<BannerCacheData>>() {
                    });
            return Optional.of(banners);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize banners from cache", e);
            return Optional.empty();
        }
    }

    /**
     * 캐시 삭제 (무효화)
     */
    public void deleteAll() {
        redisTemplate.delete(RedisKeyConstants.ACTIVE_BANNERS_KEY);
    }

    /**
     * 캐시 존재 여부 확인
     */
    public boolean exists() {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(RedisKeyConstants.ACTIVE_BANNERS_KEY));
    }
}
