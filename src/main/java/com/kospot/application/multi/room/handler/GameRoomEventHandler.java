package com.kospot.application.multi.room.handler;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.event.GameRoomJoinEvent;
import com.kospot.domain.multi.room.event.GameRoomLeaveEvent;
import com.kospot.domain.multi.room.service.GameRoomPlayerService;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.redis.common.service.SessionContextRedisService;
import com.kospot.infrastructure.websocket.domain.multi.room.service.GameRoomNotificationService;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomEventHandler {

    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomNotificationService gameRoomNotificationService;
    private final SessionContextRedisService sessionContextRedisService;
    private final GameRoomPlayerService gameRoomPlayerService;

    // can join room
    @EventListener
    public void handleJoin(GameRoomJoinEvent event) {
        GameRoom gameRoom = event.getGameRoom();
        String roomId = gameRoom.getId().toString();
        GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.from(event.getPlayer());

        // redis 업데이트
        gameRoomRedisService.addPlayerToRoom(roomId, playerInfo);

        // 입장 알림
        gameRoomNotificationService.notifyPlayerJoined(roomId, playerInfo);

        // 플레이어 업데이트
        gameRoomNotificationService.notifyPlayerListUpdated(roomId);

    }

    // leave room
    @EventListener
    public void handleLeave(GameRoomLeaveEvent event) {
        GameRoom gameRoom = event.getGameRoom();
        String roomId = gameRoom.getId().toString();
        Member player = event.getPlayer();

        // WebSocket 레벨에서 실시간 퇴장 처리 (Redis + 실시간 알림)
        gameRoomPlayerService.removePlayerFromRoom(gameRoom.getId(), player.getId());

        // 플레이어 업데이트
        gameRoomNotificationService.notifyPlayerListUpdated(roomId);
    }

}
