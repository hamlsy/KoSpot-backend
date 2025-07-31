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
    
    /**
     * 알림 타입 (PLAYER_JOINED, PLAYER_LEFT, PLAYER_KICKED, etc.)
     */
    private String type;
    
    /**
     * 게임방 ID
     */
    private String roomId;
    
    /**
     * 대상 플레이어 정보 (입장/퇴장/강퇴된 플레이어)
     */
    private GameRoomPlayerInfo playerInfo;
    
    /**
     * 현재 게임방의 모든 플레이어 목록
     */
    private List<GameRoomPlayerInfo> players;
    
    /**
     * 알림 발생 시간 (Unix timestamp)
     */
    private Long timestamp;
    
    /**
     * 알림 타입 열거형
     */
    public enum NotificationType {
        PLAYER_JOINED("플레이어 입장"),
        PLAYER_LEFT("플레이어 퇴장"),
        PLAYER_KICKED("플레이어 강퇴"),
        ROOM_SETTINGS_CHANGED("방 설정 변경"),
        GAME_STARTED("게임 시작");
        
        private final String description;
        
        NotificationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }

    public static GameRoomNotification playerJoined(String roomId, GameRoomPlayerInfo playerInfo, List<GameRoomPlayerInfo> allPlayers) {
        return GameRoomNotification.builder()
                .type(NotificationType.PLAYER_JOINED.name())
                .roomId(roomId)
                .playerInfo(playerInfo)
                .players(allPlayers)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static GameRoomNotification playerLeft(String roomId, GameRoomPlayerInfo playerInfo, List<GameRoomPlayerInfo> allPlayers) {
        return GameRoomNotification.builder()
                .type(NotificationType.PLAYER_LEFT.name())
                .roomId(roomId)
                .playerInfo(playerInfo)
                .players(allPlayers)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static GameRoomNotification playerKicked(String roomId, GameRoomPlayerInfo playerInfo, List<GameRoomPlayerInfo> allPlayers) {
        return GameRoomNotification.builder()
                .type(NotificationType.PLAYER_KICKED.name())
                .roomId(roomId)
                .playerInfo(playerInfo)
                .players(allPlayers)
                .timestamp(System.currentTimeMillis())
                .build();
    }
} 