package com.kospot.multi.room.infrastructure.redis.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class GameRoomRedisRepository {

    private static final String UPDATE_SCREEN_STATE_LUA = """
            local roomKey = KEYS[1]
            local memberId = ARGV[1]
            local newState = ARGV[2]
            local newSeq = tonumber(ARGV[3])
            local updatedAt = tonumber(ARGV[4])

            local playerJson = redis.call('HGET', roomKey, memberId)
            if not playerJson then
                return 'NOT_FOUND'
            end

            local player = cjson.decode(playerJson)
            local currentSeq = tonumber(player.screenStateSeq or 0)

            if newSeq < currentSeq then
                return 'STALE'
            end

            if newSeq == currentSeq then
                return 'NO_OP'
            end

            player.screenState = newState
            player.screenStateSeq = newSeq
            player.screenStateUpdatedAt = updatedAt

            redis.call('HSET', roomKey, memberId, cjson.encode(player))
            return 'UPDATED'
            """;

    private static final String PROMOTE_JOINING_TO_ROOM_LUA = """
            local roomKey = KEYS[1]
            local memberId = ARGV[1]
            local updatedAt = tonumber(ARGV[2])

            local playerJson = redis.call('HGET', roomKey, memberId)
            if not playerJson then
                return 'NOT_FOUND'
            end

            local player = cjson.decode(playerJson)
            local currentState = player.screenState
            local currentSeq = tonumber(player.screenStateSeq or 0)

            if currentState ~= 'JOINING' then
                return 'NO_OP'
            end

            player.screenState = 'ROOM'
            player.screenStateSeq = currentSeq
            player.screenStateUpdatedAt = updatedAt

            redis.call('HSET', roomKey, memberId, cjson.encode(player))
            return 'UPDATED'
            """;

    private static final DefaultRedisScript<String> UPDATE_SCREEN_STATE_SCRIPT = new DefaultRedisScript<>();
    private static final DefaultRedisScript<String> PROMOTE_JOINING_TO_ROOM_SCRIPT = new DefaultRedisScript<>();

    static {
        UPDATE_SCREEN_STATE_SCRIPT.setScriptText(UPDATE_SCREEN_STATE_LUA);
        UPDATE_SCREEN_STATE_SCRIPT.setResultType(String.class);

        PROMOTE_JOINING_TO_ROOM_SCRIPT.setScriptText(PROMOTE_JOINING_TO_ROOM_LUA);
        PROMOTE_JOINING_TO_ROOM_SCRIPT.setResultType(String.class);
    }

    private final RedisTemplate<String, String> redisTemplate;

    public enum ScreenStateUpdateResult {
        UPDATED,
        NO_OP,
        STALE,
        NOT_FOUND
    }

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

    public ScreenStateUpdateResult updatePlayerScreenStateIfNewer(
            String roomKey,
            String memberId,
            String newState,
            long newSeq,
            long updatedAt
    ) {
        String result = redisTemplate.execute(
                UPDATE_SCREEN_STATE_SCRIPT,
                Collections.singletonList(roomKey),
                memberId,
                newState,
                String.valueOf(newSeq),
                String.valueOf(updatedAt)
        );

        if (result == null) {
            return ScreenStateUpdateResult.NOT_FOUND;
        }

        try {
            return ScreenStateUpdateResult.valueOf(result);
        } catch (IllegalArgumentException e) {
            return ScreenStateUpdateResult.NOT_FOUND;
        }
    }

    public ScreenStateUpdateResult promotePlayerToRoomIfJoining(
            String roomKey,
            String memberId,
            long updatedAt
    ) {
        String result = redisTemplate.execute(
                PROMOTE_JOINING_TO_ROOM_SCRIPT,
                Collections.singletonList(roomKey),
                memberId,
                String.valueOf(updatedAt)
        );

        if (result == null) {
            return ScreenStateUpdateResult.NOT_FOUND;
        }

        try {
            return ScreenStateUpdateResult.valueOf(result);
        } catch (IllegalArgumentException e) {
            return ScreenStateUpdateResult.NOT_FOUND;
        }
    }

}
