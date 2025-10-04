package com.kospot.domain.multigame.room.vo;

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

    /**
     * 공통 팩토리 메서드
     */
    private static GameRoomNotification of(GameRoomNotificationType type,
                                           String roomId,
                                           GameRoomPlayerInfo playerInfo,
                                           List<GameRoomPlayerInfo> players) {
        return GameRoomNotification.builder()
                .type(type.name())
                .roomId(roomId)
                .playerInfo(playerInfo)
                .players(players)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 개별 이벤트 알림
     */
    public static GameRoomNotification playerJoined(String roomId, GameRoomPlayerInfo playerInfo) {
        return of(GameRoomNotificationType.PLAYER_JOINED, roomId, playerInfo, null);
    }

    public static GameRoomNotification playerLeft(String roomId, GameRoomPlayerInfo playerInfo) {
        return of(GameRoomNotificationType.PLAYER_LEFT, roomId, playerInfo, null);
    }

    public static GameRoomNotification playerKicked(String roomId, GameRoomPlayerInfo playerInfo) {
        return of(GameRoomNotificationType.PLAYER_KICKED, roomId, playerInfo, null);
    }

    /**
     * 전체 플레이어 동기화 알림
     */
    public static GameRoomNotification playerListUpdated(String roomId, List<GameRoomPlayerInfo> allPlayers) {
        return of(GameRoomNotificationType.PLAYER_LIST_UPDATED, roomId, null, allPlayers);
    }

    /**
     * 방 설정 변경 알림 (추후 확장)
     */
//    public static GameRoomNotification settingChanged(String roomId, Object settingPayload) {
//        // settingPayload → 별도 DTO로 관리하는 게 바람직 (예: GameRoomSettingChangedMessage)
//        return of(GameRoomNotificationType.SETTING_CHANGED, roomId, null, null);
//    }
} 