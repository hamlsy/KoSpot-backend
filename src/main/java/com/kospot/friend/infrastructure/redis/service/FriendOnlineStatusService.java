package com.kospot.friend.infrastructure.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendOnlineStatusService {

    private static final String MEMBER_STATE_KEY_PATTERN = "websocket:connection:state:member:%d";
    private static final String STATE_FIELD = "state";
    private static final String CONNECTED_STATE = "CONNECTED";

    private final StringRedisTemplate redisTemplate;

    public Set<Long> getOnlineMemberIds(Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> targetMemberIds = memberIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        if (targetMemberIds.isEmpty()) {
            return Collections.emptySet();
        }

        StringRedisSerializer serializer = (StringRedisSerializer) redisTemplate.getStringSerializer();
        List<Object> states;
        try {
            states = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                requestStates(connection, serializer, targetMemberIds);
                return null;
            }, serializer);
        } catch (Exception e) {
            log.warn("Failed to read friend online states from redis. targetSize={}", targetMemberIds.size(), e);
            return Collections.emptySet();
        }

        Set<Long> onlineMemberIds = new LinkedHashSet<>();
        for (int i = 0; i < targetMemberIds.size(); i++) {
            Object stateRaw = i < states.size() ? states.get(i) : null;
            if (CONNECTED_STATE.equals(stateRaw)) {
                onlineMemberIds.add(targetMemberIds.get(i));
            }
        }
        return onlineMemberIds;
    }

    private void requestStates(RedisConnection connection, StringRedisSerializer serializer, List<Long> memberIds) {
        byte[] stateField = serializer.serialize(STATE_FIELD);
        if (stateField == null) {
            return;
        }

        for (Long memberId : memberIds) {
            String key = String.format(MEMBER_STATE_KEY_PATTERN, memberId);
            byte[] keyBytes = serializer.serialize(key);
            if (keyBytes == null) {
                continue;
            }
            connection.hashCommands().hGet(keyBytes, stateField);
        }
    }
}
