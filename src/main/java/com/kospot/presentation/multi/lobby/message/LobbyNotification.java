package com.kospot.presentation.multi.lobby.message;

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

    public static LobbyNotification roomUpdated(GameRoomUpdateMessage gameRoomUpdateMessage) {
        return LobbyNotification.builder()
                .type(LobbyNotificationType.ROOM_UPDATED)
                .roomId(Long.parseLong(gameRoomUpdateMessage.getRoomId()))
                .timestamp(System.currentTimeMillis())
                .build();
    }



    enum LobbyNotificationType {
        ROOM_CREATED,
        ROOM_UPDATED,
        ROOM_DELETED,
    }

}
