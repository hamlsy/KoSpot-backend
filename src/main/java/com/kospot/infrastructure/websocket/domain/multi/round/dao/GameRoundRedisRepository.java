package com.kospot.infrastructure.websocket.domain.multi.round.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GameRoundRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private String getRoundKey(String gameId, String roundId) {
        return String.format("game:%s:round:%s", gameId, roundId);
    }

    public void saveRound() {
        String roundKey = getRoundKey();
    }

}
