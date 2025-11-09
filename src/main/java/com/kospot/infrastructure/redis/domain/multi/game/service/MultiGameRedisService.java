package com.kospot.infrastructure.redis.domain.multi.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiGameRedisService {

    private static final String GAME_LOADING_STATUS_KEY = "game:multi:%s:loading";
    private static final String CURRENT_GAME_KEY = "game:room:%s:current";
    private static final int GAME_DATA_EXPIRY_HOURS = 1;

    private final RedisTemplate<String, String> redisTemplate;

    public void markPlayerLoadingReady(String roomId, Long roundId, Long memberId, Long acknowledgedAt) {
        String key = getLoadingStatusKey(roomId);
        try {
            redisTemplate.opsForHash().put(key, memberId.toString(), acknowledgedAt.toString());
            redisTemplate.expire(key, GAME_DATA_EXPIRY_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Failed to mark player loading ready - RoomId: {}, RoundId: {}, MemberId: {}", roomId, roundId, memberId, e);
        }
    }

    /**
     * 현재 라운드의 로딩 상태를 조회한다.
     */
    public Map<Long, Long> getPlayerLoadingStatus(String roomId) {
        String key = getLoadingStatusKey(roomId);
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (entries == null || entries.isEmpty()) {
                return Map.of();
            }
            return entries.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> Long.parseLong(entry.getKey().toString()),
                            entry -> Long.parseLong(entry.getValue().toString())
                    ));
        } catch (Exception e) {
            log.error("Failed to get player loading status - RoomId: {}", roomId, e);
            return Map.of();
        }
    }

    /**
     * 로딩 상태 해시를 삭제한다.
     */
    public void resetLoadingStatus(String roomId) {
        String key = getLoadingStatusKey(roomId);
        redisTemplate.delete(key);
    }

    /**
     * 현재 방에서 진행 중인 게임 ID를 저장한다.
     */
    public void setCurrentGameId(String roomId, Long gameId) {
        String key = getCurrentGameKey(roomId);
        redisTemplate.opsForValue().set(key, gameId.toString(), GAME_DATA_EXPIRY_HOURS, TimeUnit.HOURS);
    }

    /**
     * 현재 방에서 진행 중인 게임 ID를 가져온다.
     */
    public Long getCurrentGameId(String roomId) {
        String key = getCurrentGameKey(roomId);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid current game id found in redis - RoomId: {}, Value: {}", roomId, value);
            redisTemplate.delete(key);
            return null;
        }
    }

    /**
     * 현재 진행 중인 게임 ID를 초기화한다.
     */
    public void clearCurrentGameId(String roomId) {
        redisTemplate.delete(getCurrentGameKey(roomId));
    }

    private String getLoadingStatusKey(String roomId) {
        return String.format(GAME_LOADING_STATUS_KEY, roomId);
    }

    private String getCurrentGameKey(String roomId) {
        return String.format(CURRENT_GAME_KEY, roomId);
    }
}

