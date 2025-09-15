package com.kospot.infrastructure.redis.domain.gametimer.service;

import com.kospot.domain.multigame.timer.vo.TimerData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameTimerService {

    //todo refactor 쓰기 연산 증가, TTL 고려
    private final RedisTemplate<String, String> redisTemplate;

    private String getTimerKey(String roomId, String roundId) {
        return "game:timer:" + roomId + ":" + roundId;
    }

    private String getRunningKey(String roomId, String roundId) {
        return "game:timer:running:" + roomId + ":" + roundId;
    }

    public void startRound(String roomId, String roundId, long roundTimeMs) {
        redisTemplate.opsForValue().set(getTimerKey(roomId, roundId), String.valueOf(roundTimeMs));
        redisTemplate.opsForValue().set(getRunningKey(roomId, roundId), "true");
    }

    public void endRound(String roomId, String roundId) {
        redisTemplate.opsForValue().set(getTimerKey(roomId, roundId), "0");
        redisTemplate.opsForValue().set(getRunningKey(roomId, roundId), "false");
    }

    public boolean isRunning(String roomId, String roundId) {
        String val = redisTemplate.opsForValue().get(getRunningKey(roomId, roundId));
        return "true".equals(val);
    }

    public long getRemainingTimeMs(String roomId, String roundId) {
        String val = redisTemplate.opsForValue().get(getTimerKey(roomId, roundId));
        return val != null ? Long.parseLong(val) : 0L;
    }

    public void decrementTimer(String roomId, String roundId) {
        long remaining = getRemainingTimeMs(roomId, roundId) - 1000;
        if (remaining <= 0) {
            endRound(roomId, roundId);
        } else {
            redisTemplate.opsForValue().set(getTimerKey(roomId, roundId), String.valueOf(remaining));
        }
    }
}
