package com.kospot.domain.multi.room.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameRoomJoinEvent {

    private Long roomId;

    private Long memberId;
    private String nickname;
    private String markerImageUrl;
    private String team;
    private boolean isHost;

}
