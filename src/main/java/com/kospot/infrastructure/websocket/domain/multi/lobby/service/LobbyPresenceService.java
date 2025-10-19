package com.kospot.infrastructure.websocket.domain.multi.lobby.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import static com.kospot.infrastructure.redis.common.constants.RedisKeyConstants.REDIS_LOBBY_USERS;

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

}
