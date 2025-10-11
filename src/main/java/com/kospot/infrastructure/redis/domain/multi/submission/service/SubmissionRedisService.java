package com.kospot.infrastructure.redis.domain.multi.submission.service;

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

    private static final String SUBMISSION_COUNT_KEY = "game:round:%s:submission:count";
    private static final String SUBMITTED_PLAYERS_KEY = "game:round:%s:submitted:players";

    private static final int ROUND_DATA_EXPIRY_MINUTES = 10;

    public Long recordSubmission(Long roundId, Long playerId) {
        String playersKey = String.format(SUBMITTED_PLAYERS_KEY, roundId);
        String countKey = String.format(SUBMISSION_COUNT_KEY, roundId);

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
        return getCurrentSubmissionCount(roundId);
    }

    public long getCurrentSubmissionCount(Long roundId) {
        String countKey = String.format(SUBMISSION_COUNT_KEY, roundId);
        String count = redisTemplate.opsForValue().get(countKey);
        return count != null ? Long.parseLong(count) : 0L;
    }

    public boolean hasPlayerSubmitted(Long roundId, Long playerId) {
        String playersKey = String.format(SUBMITTED_PLAYERS_KEY, roundId);
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(playersKey, playerId));
    }

    public void initializeRound(Long roundId) {
        String countKey = String.format(SUBMISSION_COUNT_KEY, roundId);
        String playersKey = String.format(SUBMITTED_PLAYERS_KEY, roundId);

        redisTemplate.opsForValue().set(countKey, "0");
        redisTemplate.delete(playersKey);
    }

    public void cleanupRound(Long roundId) {
        String countKey = String.format(SUBMISSION_COUNT_KEY, roundId);
        String playersKey = String.format(SUBMITTED_PLAYERS_KEY, roundId);

        redisTemplate.delete(countKey);
        redisTemplate.delete(playersKey);
    }

}
