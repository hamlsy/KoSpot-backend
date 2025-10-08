package com.kospot.presentation.multi.gameroom.dto.message;

import com.kospot.domain.multi.room.vo.GameRoomUpdateInfo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameRoomUpdateMessage {

    private final String roomId;
    private final String title;
    private final String gameModeKey;
    private final String playerMatchTypeKey;
    private final boolean privateRoom;
    private final int teamCount;

    public static GameRoomUpdateMessage of(GameRoomUpdateInfo vo) {
        return GameRoomUpdateMessage.builder()
                .roomId(vo.getRoomId())
                .title(vo.getTitle())
                .gameModeKey(vo.getGameModeKey())
                .playerMatchTypeKey(vo.getPlayerMatchTypeKey())
                .privateRoom(vo.isPrivateRoom())
                .teamCount(vo.getTeamCount())
                .build();
    }
}
