package com.kospot.multi.room.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameRoomJoinEvent {

    private Long roomId;

    private Long memberId;

}
