package com.kospot.infrastructure.redis.domain.gametimer.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameTimerService {

    private final RedisTemplate<String, String> redisTemplate;

    //todo refactoring redis
    private static final long ROUND_DURATION_MS = 2 * 60 * 1000; // 2분

    private String timerKey(String roomId) { return "game:timer:" + roomId; }
    private String runningKey(String roomId) { return "game:timer:running:" + roomId; }

    public void startRound(String roomId, long roundTimeMs) {
        redisTemplate.opsForValue().set(timerKey(roomId), String.valueOf(roundTimeMs));
        redisTemplate.opsForValue().set(runningKey(roomId), "true");
    }

    public void endRound(String roomId) {
        redisTemplate.opsForValue().set(timerKey(roomId), "0");
        redisTemplate.opsForValue().set(runningKey(roomId), "false");
    }

    public boolean isRunning(String roomId) {
        String val = redisTemplate.opsForValue().get(runningKey(roomId));
        return "true".equals(val);
    }

    public long getRemainingTimeMs(String roomId) {
        String val = redisTemplate.opsForValue().get(timerKey(roomId));
        return val != null ? Long.parseLong(val) : 0L;
    }

    public void decrementTimer(String roomId) {
        long remaining = getRemainingTimeMs(roomId) - 1000;
        if(remaining <= 0) {
            endRound(roomId);
        } else{
            redisTemplate.opsForValue().set(timerKey(roomId), String.valueOf(remaining));
        }
    }

}
