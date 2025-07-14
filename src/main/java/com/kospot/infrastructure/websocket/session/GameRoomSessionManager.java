package com.kospot.infrastructure.websocket.session;

import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.constants.WebSocketChannelConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomSessionManager {

    private static final String ROOM_PLAYERS_KEY = "game:room:%s:players";
    private static final String ROOM_BANNED_KEY = "game:room:%s:banned";
    private static final String ROOM_HOST_KEY = "game:room:%s:host";
    private static final String ROOM_SETTINGS_KEY = "game:room:%s:settings";
    private static final String PLAYER_SESSION_KEY = "game:player:%s:session";
    private static final String SESSION_SUBSCRIPTIONS_KEY = "game:session:%s:subscriptions";

    private static final int SESSION_EXPIRY_HOURS = 24;

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public void addSubscription(WebSocketMemberPrincipal principal, String roomId, String destination, String sessionId) {
        String sessionKey = String.format(SESSION_SUBSCRIPTIONS_KEY, sessionId);
        redisTemplate.opsForSet().add(sessionKey, destination);
        redisTemplate.expire(sessionKey, SESSION_EXPIRY_HOURS, TimeUnit.HOURS);

        //player session
        String playerSessionKey = String.format(PLAYER_SESSION_KEY, principal.getMemberId());
        redisTemplate.opsForValue().set(playerSessionKey, sessionId, SESSION_EXPIRY_HOURS, TimeUnit.HOURS);

        // 게임방 플레이어 목록 구독인 경우 플레이어 추가
        if (destination.contains("/players")) {
            addPlayerToRoom(principal, roomId);
        }

    }

    private void addPlayerToRoom(WebSocketMemberPrincipal principal, String roomId) {
        String roomKey = String.format(ROOM_PLAYERS_KEY, roomId);
        redisTemplate.opsForHash().put(roomKey, principal.getMemberId().toString(), player.toJson()); // key -> memberId

        // 다른 플레이어들에게 입장 알림
        notifyPlayerJoined(roomId, player);
    }

    //todo gameplayer -> member
    private void notifyPlayerJoined(String roomId, Object object) {
        String destination = String.format(WebSocketChannelConstants.GAME_ROOM_PLAYER_LIST, roomId);
    }

}
