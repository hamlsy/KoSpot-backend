package com.kospot.common.websocket.domain.multi.lobby.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.kospot.common.redis.common.constants.RedisKeyConstants.REDIS_LOBBY_USERS;

@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyPresenceService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void joinGlobalLobby(Long memberId, String sessionId) {
        redisTemplate.opsForHash().put(REDIS_LOBBY_USERS, sessionId, memberId.toString());
        log.info("User {} joined global lobby with session {}", memberId, sessionId);
    }

    public void leaveGlobalLobby(String sessionId) {
        redisTemplate.opsForHash().delete(REDIS_LOBBY_USERS, sessionId);
        log.info("User {} left global lobby", sessionId);
    }

    public long getLobbyUserCount() {
        return redisTemplate.opsForHash().size(REDIS_LOBBY_USERS);
    }

    public Set<Long> getOnlineMemberIds() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(REDIS_LOBBY_USERS);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptySet();
        }

        return entries.values().stream()
                .map(Object::toString)
                .map(this::parseLong)
                .filter(v -> v != null)
                .collect(Collectors.toSet());
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
