package com.kospot.infrastructure.redis.domain.multi.room.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class GameRoomRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    public void savePlayer(String roomKey, String memberId, String playerJson, long expireHours) {
        redisTemplate.opsForHash().put(roomKey, memberId, playerJson);
        redisTemplate.expire(roomKey, expireHours, TimeUnit.HOURS);
    }

    public String findPlayer(String roomKey, String memberId) {
        return (String) redisTemplate.opsForHash().get(roomKey, memberId);
    }

    public void deletePlayer(String roomKey, String memberId) {
        redisTemplate.opsForHash().delete(roomKey, memberId);
    }

    public Map<Object, Object> findAllPlayers(String roomKey) {
        return redisTemplate.opsForHash().entries(roomKey);
    }

    public Long getPlayerCount(String roomKey) {
        return redisTemplate.opsForHash().size(roomKey);
    }

    public void putAllPlayers(String roomKey, Map<Object, Object> players) {
        redisTemplate.opsForHash().putAll(roomKey, players);
    }

    public int countPlayers(String roomKey) {
        Long count = redisTemplate.opsForHash().size(roomKey);
        return count.intValue();
    }

}
