package com.kospot.infrastructure.websocket.domain.gameroom.service;


import com.kospot.domain.multi.room.vo.GameRoomNotification;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.websocket.domain.multi.room.constants.GameRoomChannelConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GameRoomPlayersBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameRoomRedisService gameRoomRedisService;

    // todo refactor
    // 단일 서버 기준
    @Scheduled(fixedRate = 10000) // 10초마다
    public void broadcastAllRooms() {
        Set<String> roomIds = gameRoomRedisService.getActiveRoomKeys();
        roomIds.stream().parallel().forEach(this::broadcastRoom);
    }

    public void broadcastRoom(String roomId) {
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(roomId);
        GameRoomNotification notification = GameRoomNotification.playerListUpdated(roomId, players);
        messagingTemplate.convertAndSend(GameRoomChannelConstants.getGameRoomPlayerListChannel(roomId), notification);
    }

}
