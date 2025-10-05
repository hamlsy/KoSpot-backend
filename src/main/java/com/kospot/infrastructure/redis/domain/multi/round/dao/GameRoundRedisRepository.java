package com.kospot.infrastructure.redis.domain.multi.round.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.domain.multi.round.entity.BaseGameRound;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class GameRoundRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String ROUND_META_KEY = "game:%s:round:%s"; // 게임ID:라운드ID
    private static final String ROUND_DETAIL_KEY = "game:%s:round:%s:%s-detail"; // 타입별 상세정보
    private static final String PLAYER_ROUND_KEY = "player:%s:game:%s:round"; // 플레이어별 현재 라운드
    private static final String ACTIVE_ROUND_INDEX = "game:active:rounds"; // ZSet

    private static final Duration BUFFER_DURATION = Duration.ofMinutes(5);

    /**
     * 라운드 저장 with TTL
     */
    public void saveRound(String gameId, BaseGameRound round) {
        Duration ttl = round.getDuration().plus(BUFFER_DURATION);

        // 1️⃣ UUID 기반 라운드 Key 생성
        String roundUuid = UUID.randomUUID().toString();

        // 1️⃣ 공통 메타데이터 저장 (HASH)
        String metaKey = String.format(ROUND_META_KEY, gameId, roundUuid);
        Map<String, Object> meta = Map.of(
                "roundUuid", roundUuid,
                "roundNumber", round.getRoundNumber(),
                "type", round.getType().name(),
                "isFinished", round.getIsFinished()
        );

        // 활성 라운드 인덱스에 추가 (Sorted Set - 종료 시간 기준)
        long endTimestamp = round.getServerStartTime()
                .plus(round.getDuration())
                .toEpochMilli();

        redisTemplate.opsForZSet().add(
                ROUND_INDEX,
                round.getRoundId(),
                endTimestamp
        );

        // 플레이어별 현재 라운드 매핑
        round.getPlayerIds().forEach(playerId -> {
            String playerKey = String.format(PLAYER_ROUND, playerId);
            redisTemplate.opsForValue().set(
                    playerKey,
                    round.getRoundId(),
                    round.getDuration().plusMinutes(5)
            );
        });
    }

}
