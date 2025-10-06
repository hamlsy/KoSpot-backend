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



}
