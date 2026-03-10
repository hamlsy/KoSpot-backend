package com.kospot.common.websocket.connection.service;

import com.kospot.common.redis.common.service.SessionContextRedisService;
import com.kospot.common.websocket.config.WebSocketHeartbeatProperties;
import com.kospot.common.websocket.connection.domain.WebSocketConnectionState;
import com.kospot.common.websocket.session.service.WebSocketSessionService;
import com.kospot.friend.infrastructure.websocket.service.FriendChatSubscriptionCacheService;
import com.kospot.multi.room.application.usecase.LeaveGameRoomUseCase;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class WebSocketConnectionStateOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConnectionStateOrchestrator.class);

    private static final String MEMBER_STATE_KEY_PATTERN = "websocket:connection:state:member:%d";

    private final LeaveGameRoomUseCase leaveGameRoomUseCase;
    private final SessionContextRedisService sessionContextRedisService;
    private final WebSocketSessionService webSocketSessionService;
    private final FriendChatSubscriptionCacheService friendChatSubscriptionCacheService;
    private final RedisTemplate<String, String> redisTemplate;
    private final WebSocketHeartbeatProperties webSocketHeartbeatProperties;
    private final TaskScheduler webSocketHeartbeatTaskScheduler;

    public WebSocketConnectionStateOrchestrator(
            LeaveGameRoomUseCase leaveGameRoomUseCase,
            SessionContextRedisService sessionContextRedisService,
            WebSocketSessionService webSocketSessionService,
            FriendChatSubscriptionCacheService friendChatSubscriptionCacheService,
            RedisTemplate<String, String> redisTemplate,
            WebSocketHeartbeatProperties webSocketHeartbeatProperties,
            @Qualifier("webSocketHeartbeatTaskScheduler")
            TaskScheduler webSocketHeartbeatTaskScheduler) {
        this.leaveGameRoomUseCase = leaveGameRoomUseCase;
        this.sessionContextRedisService = sessionContextRedisService;
        this.webSocketSessionService = webSocketSessionService;
        this.friendChatSubscriptionCacheService = friendChatSubscriptionCacheService;
        this.redisTemplate = redisTemplate;
        this.webSocketHeartbeatProperties = webSocketHeartbeatProperties;
        this.webSocketHeartbeatTaskScheduler = webSocketHeartbeatTaskScheduler;
    }

    public void handleReconnect(Long memberId, String sessionId) {
        if (memberId == null || memberId <= 0) {
            return;
        }

        String memberStateKey = stateKey(memberId);
        Object stateRaw = redisTemplate.opsForHash().get(memberStateKey, "state");
        String now = String.valueOf(System.currentTimeMillis());

        if (stateRaw == null) {
            redisTemplate.opsForHash().put(memberStateKey, "state", WebSocketConnectionState.CONNECTED.name());
            redisTemplate.opsForHash().put(memberStateKey, "sessionId", sessionId);
            redisTemplate.opsForHash().put(memberStateKey, "connectedAt", now);
            refreshMemberStateTtl(memberStateKey);
            return;
        }

        String currentState = stateRaw.toString();
        if (WebSocketConnectionState.DISCONNECTED_TEMP.name().equals(currentState)) {
            redisTemplate.opsForHash().put(memberStateKey, "state", WebSocketConnectionState.RECONNECTED.name());
            redisTemplate.opsForHash().put(memberStateKey, "reconnectedAt", now);
            redisTemplate.opsForHash().put(memberStateKey, "sessionId", sessionId);
            redisTemplate.opsForHash().delete(memberStateKey, "disconnectToken");

            redisTemplate.opsForHash().put(memberStateKey, "state", WebSocketConnectionState.CONNECTED.name());
            redisTemplate.opsForHash().put(memberStateKey, "connectedAt", now);
            redisTemplate.opsForHash().delete(memberStateKey, "disconnectedAt");
            redisTemplate.opsForHash().delete(memberStateKey, "graceExpireAt");

            refreshMemberStateTtl(memberStateKey);
            log.info("Connection recovered within grace period - MemberId: {}, SessionId: {}", memberId, sessionId);
            return;
        }

        redisTemplate.opsForHash().put(memberStateKey, "state", WebSocketConnectionState.CONNECTED.name());
        redisTemplate.opsForHash().put(memberStateKey, "sessionId", sessionId);
        redisTemplate.opsForHash().put(memberStateKey, "connectedAt", now);
        refreshMemberStateTtl(memberStateKey);
    }

    public void handleDisconnect(Long memberId, String sessionId, Long gameRoomId, String reason, boolean skipRoomLeave) {
        cleanupDisconnectedSession(sessionId);

        if (memberId == null || memberId <= 0) {
            return;
        }

        String memberStateKey = stateKey(memberId);
        long disconnectedAt = System.currentTimeMillis();
        long gracePeriodMs = webSocketHeartbeatProperties.getHeartbeat().getGracePeriodMs();
        long graceExpireAt = disconnectedAt + gracePeriodMs;
        String disconnectToken = UUID.randomUUID().toString();

        redisTemplate.opsForHash().put(memberStateKey, "state", WebSocketConnectionState.DISCONNECTED_TEMP.name());
        redisTemplate.opsForHash().put(memberStateKey, "sessionId", sessionId);
        redisTemplate.opsForHash().put(memberStateKey, "reason", reason);
        redisTemplate.opsForHash().put(memberStateKey, "disconnectedAt", String.valueOf(disconnectedAt));
        redisTemplate.opsForHash().put(memberStateKey, "graceExpireAt", String.valueOf(graceExpireAt));
        redisTemplate.opsForHash().put(memberStateKey, "disconnectToken", disconnectToken);
        redisTemplate.opsForHash().put(memberStateKey, "skipRoomLeave", String.valueOf(skipRoomLeave));
        if (gameRoomId != null) {
            redisTemplate.opsForHash().put(memberStateKey, "gameRoomId", String.valueOf(gameRoomId));
        } else {
            redisTemplate.opsForHash().delete(memberStateKey, "gameRoomId");
        }
        refreshMemberStateTtl(memberStateKey);

        log.info("Connection marked as DISCONNECTED_TEMP - MemberId: {}, SessionId: {}, RoomId: {}, GraceMs: {}, Reason: {}",
                memberId, sessionId, gameRoomId, gracePeriodMs, reason);

        webSocketHeartbeatTaskScheduler.schedule(
                () -> finalizeDisconnectIfStillStale(memberId, disconnectToken),
                Instant.ofEpochMilli(graceExpireAt));
    }

    private void finalizeDisconnectIfStillStale(Long memberId, String expectedDisconnectToken) {
        String memberStateKey = stateKey(memberId);

        Object tokenRaw = redisTemplate.opsForHash().get(memberStateKey, "disconnectToken");
        if (tokenRaw == null || !expectedDisconnectToken.equals(tokenRaw.toString())) {
            return;
        }

        Object stateRaw = redisTemplate.opsForHash().get(memberStateKey, "state");
        if (stateRaw == null || !WebSocketConnectionState.DISCONNECTED_TEMP.name().equals(stateRaw.toString())) {
            return;
        }

        boolean skipRoomLeave = Boolean.parseBoolean(String.valueOf(redisTemplate.opsForHash().get(memberStateKey, "skipRoomLeave")));
        Long gameRoomId = parseLong(redisTemplate.opsForHash().get(memberStateKey, "gameRoomId"));

        if (!skipRoomLeave && gameRoomId != null) {
            try {
                leaveGameRoomUseCase.execute(memberId, gameRoomId);
                log.info("Grace expired; member left game room - MemberId: {}, RoomId: {}", memberId, gameRoomId);
            } catch (Exception e) {
                log.warn("Failed to finalize room leave after grace expiration - MemberId: {}, RoomId: {}",
                        memberId, gameRoomId, e);
            }
        } else if (skipRoomLeave) {
            log.info("Grace expired with skipRoomLeave=true - MemberId: {}, RoomId: {}", memberId, gameRoomId);
        }

        redisTemplate.opsForHash().put(memberStateKey, "state", WebSocketConnectionState.LEFT.name());
        redisTemplate.opsForHash().put(memberStateKey, "leftAt", String.valueOf(System.currentTimeMillis()));
        redisTemplate.opsForHash().delete(memberStateKey, "disconnectToken");
        refreshMemberStateTtl(memberStateKey);
    }

    private void cleanupDisconnectedSession(String sessionId) {
        webSocketSessionService.cleanupSession(sessionId);
        friendChatSubscriptionCacheService.removeSession(sessionId);
        sessionContextRedisService.removeAllAttr(sessionId);
    }

    private void refreshMemberStateTtl(String memberStateKey) {
        long gracePeriodMs = webSocketHeartbeatProperties.getHeartbeat().getGracePeriodMs();
        long ttlMs = Math.max(gracePeriodMs * 3, TimeUnit.MINUTES.toMillis(5));
        redisTemplate.expire(memberStateKey, ttlMs, TimeUnit.MILLISECONDS);
    }

    private String stateKey(Long memberId) {
        return String.format(MEMBER_STATE_KEY_PATTERN, memberId);
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
