package com.kospot.infrastructure.redis.domain.timer.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GameTimerRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String ROUND_KEY = "game:room:%s:round:%s";
    private static final String ACTIVE_ROUNDS_KEY = "game:room:%s:active:rounds";
    private static final String PLAYER_ROUND_KEY = "player:%s:room:%s:round";

    private String getRoundKey(String roomId, String roundId) {
        return String.format(ROUND_KEY, roomId, roundId);
    }

    private String getActiveKey(String roomId, String roundId) {
        return String.format(ACTIVE_ROUNDS_KEY, roomId);
    }

    public void saveRound(String roomId, Object round, String roundId,
                          Duration duration,
                          List<String> playerIds, Instant startTime) {
        String roundKey = getRoundKey(roomId, roundId);

        redisTemplate.opsForValue().set(
                roundKey,
                round,
                duration.plusMinutes(5) // TTL: 라운드 시간 + 5분
        );

        String activeKey = getActiveKey(roomId, roundId);
        long endTimestamp = startTime.plus(duration).toEpochMilli();
        redisTemplate.opsForZSet().add(activeKey, roundId, endTimestamp);

        // 플레이어별 라운드 매핑
        playerIds.forEach(playerId -> {
            String playerKey = String.format(PLAYER_ROUND_KEY, playerId, roomId);
            redisTemplate.opsForValue().set(
                    playerKey,
                    roundId,
                    duration.plusMinutes(5)
            );
        });

    }


}
