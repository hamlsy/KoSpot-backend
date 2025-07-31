package com.kospot.domain.multigame.gameRoom.event;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameRoomJoinEvent {

    private final GameRoom gameRoom;
    private final Member member;

}
