package com.kospot.presentation.multigame.gameroom.dto.event;

import com.kospot.domain.multigame.gameRoom.vo.GameRoomUpdateInfo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameRoomUpdateEvent {

    private final String roomId;
    private final String title;
    private final String gameModeKey;
    private final String playerMatchTypeKey;
    private final boolean privateRoom;
    private final int teamCount;

    public static GameRoomUpdateEvent of(GameRoomUpdateInfo vo) {
        return GameRoomUpdateEvent.builder()
                .roomId(vo.getRoomId())
                .title(vo.getTitle())
                .gameModeKey(vo.getGameModeKey())
                .playerMatchTypeKey(vo.getPlayerMatchTypeKey())
                .privateRoom(vo.isPrivateRoom())
                .teamCount(vo.getTeamCount())
                .build();
    }
}
