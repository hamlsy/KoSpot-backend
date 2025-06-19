package com.kospot.infrastructure.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionMappingService {

    private final RedisTemplate<String, Object> redisTemplate;

    // SessionId -> MemberId
    public void saveMemberSession(String sessionId, String memberId) {
        redisTemplate.opsForValue().set("session:member:" + sessionId, memberId);
        redisTemplate.opsForSet().add("member:sessions:" + memberId, sessionId);
    }

    public String getMemberIdBySessionId(String sessionId) {
        return (String) redisTemplate.opsForValue().get("session:member:" + sessionId);
    }

    public void removeMemberSession(String sessionId) {
        String memberId = getMemberIdBySessionId(sessionId);
        if (memberId != null) {
            redisTemplate.delete("session:member:" + sessionId);
            redisTemplate.opsForSet().remove("member:sessions:" + memberId, sessionId);
        }
    }

}
