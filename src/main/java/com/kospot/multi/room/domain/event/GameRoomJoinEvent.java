package com.kospot.multi.room.domain.event;

import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameRoomJoinEvent {

    private Long roomId;

    private GameRoomPlayerInfo playerInfo;

}
