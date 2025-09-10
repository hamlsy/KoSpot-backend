package com.kospot.infrastructure.websocket.domain.gameroom.service;

import com.kospot.domain.multigame.gameRoom.service.GameRoomPlayerService;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomPlayerInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PlayerListBroadcaster {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final GameRoomRedisService gameRoomRedisService;

    @Scheduled(fixedRate = 5000) // 5초마다
    public void broadcastAllRooms() {
        Set<String> roomIds = gameRoomRedisService.getActiveRoomKeys();
        roomIds.stream().parallel().forEach(this::broadcastRoom);
    }

    public void broadcastRoom(String roomId) {
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(roomId);
        simpMessagingTemplate.convertAndSend("/topic/room/" + roomId + "/players", players);
    }

}
