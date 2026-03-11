package com.kospot.multi.room.domain.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameRoomUpdateInfo {

    private final String roomId;
    private final String title;
    private final int timeLimit;
    private final int maxPlayers;
    private final String gameModeKey;
    private final String playerMatchTypeKey;
    private final boolean poiNameVisible;
    private final String password;
    private final boolean privateRoom;
    private final int teamCount;
    private final int totalRounds;

}
