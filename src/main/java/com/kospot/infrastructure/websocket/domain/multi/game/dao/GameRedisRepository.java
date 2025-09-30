package com.kospot.infrastructure.websocket.domain.multi.game.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.domain.multigame.game.entity.MultiGame;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String GAME_KEY_PREFIX = "game:";
    private static final String GAME_TYPE_PREFIX = "game:type:";

    /**
     * 게임 생성/시작 시 Redis에 저장
     */
    public void saveGame(MultiGame game) {
        String gameKey = GAME_KEY_PREFIX + game.getId();
        String typeKey = GAME_TYPE_PREFIX + game.getType().name();

        try {
            // 1. Hash 형태로 게임 정보 저장
            Map<String, String> gameMap = objectMapper.convertValue(game, new TypeReference<>() {});
            redisTemplate.opsForHash().putAll(gameKey, gameMap);

            // 2. 타입별 Set에 gameId 추가
            redisTemplate.opsForSet().add(typeKey, game.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to save game in Redis", e);
        }
    }


}
