package com.kospot.infrastructure.redis.domain.multi.submission.service;

import com.kospot.domain.game.vo.GameMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
    * Redis Keys Structure
    * is roadview mode - game:roadview:round:{roundId}:submission:count
    */

    private static final String SUBMISSION_COUNT_KEY = "game:%s:round:%s:submission:count";
    private static final String SUBMITTED_PLAYERS_KEY = "game:%s:round:%s:submitted:players";

    private static final int ROUND_DATA_EXPIRY_MINUTES = 10;

    public Long recordSubmission(GameMode mode, Long roundId, Long playerId) {
        String playersKey = getPlayersKey(mode, roundId);
        String countKey = getCountKey(mode, roundId);

        // add player in Set
        boolean isNewSubmission = Optional.ofNullable(
                redisTemplate.opsForSet().add(playersKey, playerId.toString())
        ).orElse(0L) > 0;

        if(isNewSubmission) {
            Long currentCount = redisTemplate.opsForValue().increment(countKey);

            // TTL
            redisTemplate.expire(playersKey, ROUND_DATA_EXPIRY_MINUTES, TimeUnit.MINUTES);
            redisTemplate.expire(countKey, ROUND_DATA_EXPIRY_MINUTES, TimeUnit.MINUTES);

            return currentCount;
        }
        log.warn("⚠️ Duplicate submission attempt - RoundId: {}, PlayerId: {}", roundId, playerId);
        return getCurrentSubmissionCount(mode, roundId);
    }

    public long getCurrentSubmissionCount(GameMode mode, Long roundId) {
        String countKey = getCountKey(mode, roundId);
        String count = redisTemplate.opsForValue().get(countKey);
        return count != null ? Long.parseLong(count) : 0L;
    }

    public boolean hasPlayerSubmitted(GameMode mode, Long roundId, Long playerId) {
        String playersKey = getPlayersKey(mode, roundId);
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(playersKey, playerId));
    }

    public void initializeRound(GameMode mode, Long roundId) {
        String playersKey = getPlayersKey(mode, roundId);
        String countKey = getCountKey(mode, roundId);

        redisTemplate.opsForValue().set(countKey, "0");
        redisTemplate.delete(playersKey);
    }

    public void cleanupRound(GameMode mode, Long roundId) {
        String playersKey = getPlayersKey(mode, roundId);
        String countKey = getCountKey(mode, roundId);

        redisTemplate.delete(countKey);
        redisTemplate.delete(playersKey);
    }

    public String getCountKey(GameMode mode,Long roundId) {
        return String.format(SUBMISSION_COUNT_KEY, mode.name().toLowerCase(),roundId);
    }

    public String getPlayersKey(GameMode mode,Long roundId) {
        return String.format(SUBMITTED_PLAYERS_KEY, mode.name().toLowerCase(),roundId);
    }

}
