package com.kospot.presentation.multi.lobby.message;

import com.kospot.domain.multi.room.vo.GameRoomStatus;
import com.kospot.presentation.multi.room.dto.message.GameRoomUpdateMessage;
import com.kospot.presentation.multi.room.dto.response.FindGameRoomResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LobbyNotification {

    private LobbyNotificationType type;
    private Long roomId;
    private FindGameRoomResponse room;
    private StatusUpdatedRoom statusUpdatedRoom;
    private Long timestamp;

    public static LobbyNotification roomCreated(FindGameRoomResponse room) {
        return LobbyNotification.builder()
                .type(LobbyNotificationType.ROOM_CREATED)
                .room(room)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static LobbyNotification roomDeleted(Long roomId) {
        return LobbyNotification.builder()
                .type(LobbyNotificationType.ROOM_DELETED)
                .roomId(roomId)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static LobbyNotification roomUpdated(FindGameRoomResponse room) {
        return LobbyNotification.builder()
                .type(LobbyNotificationType.ROOM_UPDATED)
                .roomId(room.getGameRoomId())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static LobbyNotification roomStatusUpdated(Long roomId, StatusUpdatedRoom room) {
        return LobbyNotification.builder()
                .type(LobbyNotificationType.ROOM_STATUS_UPDATED)
                .roomId(roomId)
                .statusUpdatedRoom(room)
                .timestamp(System.currentTimeMillis())
                .build();
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatusUpdatedRoom {
        private Long currentPlayerCount;
        private GameRoomStatus status;
    }

    enum LobbyNotificationType {
        ROOM_CREATED,
        ROOM_UPDATED, // 게임 방 정보 변경
        ROOM_STATUS_UPDATED, // 게임 방 상태 변경
        ROOM_DELETED,
    }

}
