package com.kospot.mvp.infrastructure.redis.dao;

import com.kospot.mvp.domain.vo.MvpCandidateSnapshot;
import com.kospot.gamerank.domain.vo.RankLevel;
import com.kospot.gamerank.domain.vo.RankTier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DailyMvpCandidateRedisRepository {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final String COMPARE_AND_SET_LUA = """
            local key = KEYS[1]
            local memberId = ARGV[1]
            local gameId = ARGV[2]
            local newScore = tonumber(ARGV[3])
            local newEndedAt = tonumber(ARGV[4])
            local rankTier = ARGV[5]
            local rankLevel = ARGV[6]
            local ratingScore = ARGV[7]
            local updatedAt = ARGV[8]
            local ttlSeconds = tonumber(ARGV[9])

            local currentScore = redis.call('HGET', key, 'score')
            if not currentScore then
                redis.call('HSET', key,
                    'memberId', memberId,
                    'roadViewGameId', gameId,
                    'score', ARGV[3],
                    'endedAtEpochMilli', ARGV[4],
                    'rankTier', rankTier,
                    'rankLevel', rankLevel,
                    'ratingScore', ratingScore,
                    'updatedAtEpochMilli', updatedAt)
                redis.call('EXPIRE', key, ttlSeconds)
                return 1
            end

            local currentScoreNum = tonumber(currentScore)
            local currentEndedAt = tonumber(redis.call('HGET', key, 'endedAtEpochMilli'))
            local currentGameId = tonumber(redis.call('HGET', key, 'roadViewGameId'))

            local shouldReplace = false

            if newScore > currentScoreNum then
                shouldReplace = true
            elseif newScore == currentScoreNum then
                if newEndedAt < currentEndedAt then
                    shouldReplace = true
                elseif newEndedAt == currentEndedAt and tonumber(gameId) < currentGameId then
                    shouldReplace = true
                end
            end

            if not shouldReplace then
                return 0
            end

            redis.call('HSET', key,
                'memberId', memberId,
                'roadViewGameId', gameId,
                'score', ARGV[3],
                'endedAtEpochMilli', ARGV[4],
                'rankTier', rankTier,
                'rankLevel', rankLevel,
                'ratingScore', ratingScore,
                'updatedAtEpochMilli', updatedAt)
            redis.call('EXPIRE', key, ttlSeconds)
            return 1
            """;

    private static final DefaultRedisScript<Long> COMPARE_AND_SET_SCRIPT = new DefaultRedisScript<>();

    static {
        COMPARE_AND_SET_SCRIPT.setScriptText(COMPARE_AND_SET_LUA);
        COMPARE_AND_SET_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;

    public boolean compareAndSetIfBetter(String candidateKey, MvpCandidateSnapshot snapshot, Duration ttl) {
        Long result = redisTemplate.execute(
                COMPARE_AND_SET_SCRIPT,
                Collections.singletonList(candidateKey),
                String.valueOf(snapshot.memberId()),
                String.valueOf(snapshot.roadViewGameId()),
                String.valueOf(snapshot.score()),
                String.valueOf(toEpochMilli(snapshot.endedAt())),
                snapshot.rankTier().name(),
                snapshot.rankLevel().name(),
                String.valueOf(snapshot.ratingScore()),
                String.valueOf(toEpochMilli(LocalDateTime.now(KST))),
                String.valueOf(ttl.toSeconds())
        );
        return result != null && result == 1L;
    }

    public Optional<MvpCandidateSnapshot> find(String candidateKey) {
        Map<Object, Object> hash = redisTemplate.opsForHash().entries(candidateKey);
        if (hash == null || hash.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new MvpCandidateSnapshot(
                Long.parseLong(String.valueOf(hash.get("memberId"))),
                Long.parseLong(String.valueOf(hash.get("roadViewGameId"))),
                Double.parseDouble(String.valueOf(hash.get("score"))),
                fromEpochMilli(Long.parseLong(String.valueOf(hash.get("endedAtEpochMilli")))),
                RankTier.valueOf(String.valueOf(hash.get("rankTier"))),
                RankLevel.valueOf(String.valueOf(hash.get("rankLevel"))),
                Integer.parseInt(String.valueOf(hash.get("ratingScore")))
        ));
    }

    private long toEpochMilli(LocalDateTime time) {
        return time.atZone(KST).toInstant().toEpochMilli();
    }

    private LocalDateTime fromEpochMilli(long epochMilli) {
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMilli), KST);
    }
}
