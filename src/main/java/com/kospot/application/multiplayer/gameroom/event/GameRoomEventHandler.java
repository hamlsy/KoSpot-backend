package com.kospot.application.multiplayer.gameroom.event;

import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.event.GameRoomJoinEvent;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomNotificationService;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomRedisService;
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

    @EventListener
    public void handleJoinV1(GameRoomJoinEvent event) {
        GameRoom gameRoom = event.getGameRoom();
        String roomId = gameRoom.getId().toString();
        GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.from(event.getPlayer());

        gameRoomRedisService.canJoinRoom(roomId, gameRoom.getMaxPlayers());

        // redis 업데이트
        gameRoomRedisService.addPlayerToRoom(roomId, playerInfo);

        // 입장 알림
        gameRoomNotificationService.notifyPlayerJoined(roomId, playerInfo);
    }

}
