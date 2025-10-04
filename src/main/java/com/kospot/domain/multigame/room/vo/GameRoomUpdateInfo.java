package com.kospot.domain.multigame.room.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameRoomUpdateInfo {

    private final String roomId;
    private final String title;
    private final String gameModeKey;
    private final String playerMatchTypeKey;
    private final String password;
    private final boolean privateRoom;
    private final int teamCount;

}
