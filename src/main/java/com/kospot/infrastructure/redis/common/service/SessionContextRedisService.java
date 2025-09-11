package com.kospot.infrastructure.redis.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionContextRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String KEY_PATTERN = "session:ctx:%s";
    private static final int SESSION_EXPIRE_HOURS = 24;

    /**
     *  사용 예시
     * 게임 입장: setAttr(sessionId, "roomId", roomId)
     * 채팅 입장: setAttr(sessionId, "chatId", chatId)
     * session disconnect 시 getAttr(sessionId, "roomId", String.class)로 역참조 → 매핑 해제
     * @param sessionId
     * @param attrKey
     * @param value
     * @param <T>
     */

    // 개별 attribute 저장/수정
    public <T> void setAttr(String sessionId, String attrKey, T value) {
        String redisKey = String.format(KEY_PATTERN, sessionId);
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForHash().put(redisKey, attrKey, json);
            redisTemplate.expire(redisKey, SESSION_EXPIRE_HOURS, TimeUnit.HOURS); // 필요시 TTL 갱신
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis에 attr 저장 실패", e);
        }
    }

    // 개별 attribute 조회 (타입 변환 지원)
    public <T> T getAttr(String sessionId, String attrKey, Class<T> type) {
        String redisKey = String.format(KEY_PATTERN, sessionId);
        Object value = redisTemplate.opsForHash().get(redisKey, attrKey);
        if (value == null) return null;
        try {
            return objectMapper.readValue(value.toString(), type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis에서 attr 조회 실패", e);
        }
    }

    // 개별 attribute 삭제
    public void removeAttr(String sessionId, String attrKey) {
        String redisKey = String.format(KEY_PATTERN, sessionId);
        redisTemplate.opsForHash().delete(redisKey, attrKey);
    }

    // 전체 컨텍스트 한번에 모두 조회(Hash 전체)
    public Map<String, Object> getAllAttrs(String sessionId) {
        String redisKey = String.format(KEY_PATTERN, sessionId);
        Map<Object, Object> map = redisTemplate.opsForHash().entries(redisKey);
        // 역직렬화 생략: attr마다 타입 필요, 실무에서는 각각 파싱 utility 사용
        return (Map) map;
    }
}