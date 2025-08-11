package com.kospot.domain.multigame.gameRoom.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 게임방 실시간 알림 VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameRoomNotification {

    private String type;

    private String roomId;

    private GameRoomPlayerInfo playerInfo;

    private List<GameRoomPlayerInfo> players;

    private Long timestamp;

    public static GameRoomNotification playerJoined(String roomId, GameRoomPlayerInfo playerInfo, List<GameRoomPlayerInfo> allPlayers) {
        return GameRoomNotification.builder()
                .type(GameRoomNotificationType.PLAYER_JOINED.name())
                .roomId(roomId)
                .playerInfo(playerInfo)
                .players(allPlayers)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static GameRoomNotification playerLeft(String roomId, GameRoomPlayerInfo playerInfo, List<GameRoomPlayerInfo> allPlayers) {
        return GameRoomNotification.builder()
                .type(GameRoomNotificationType.PLAYER_LEFT.name())
                .roomId(roomId)
                .playerInfo(playerInfo)
                .players(allPlayers)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static GameRoomNotification playerKicked(String roomId, GameRoomPlayerInfo playerInfo, List<GameRoomPlayerInfo> allPlayers) {
        return GameRoomNotification.builder()
                .type(GameRoomNotificationType.PLAYER_KICKED.name())
                .roomId(roomId)
                .playerInfo(playerInfo)
                .players(allPlayers)
                .timestamp(System.currentTimeMillis())
                .build();
    }
} 