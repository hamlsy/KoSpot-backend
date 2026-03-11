package com.kospot.multi.room.domain.event;

import com.kospot.multi.room.application.vo.LeaveDecision;
import com.kospot.member.domain.entity.Member;
import com.kospot.multi.room.domain.entity.GameRoom;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameRoomLeaveEvent {

    private final GameRoom gameRoom;
    private final Member leavingMember;
    private final LeaveDecision decision;
    private final GameRoomPlayerInfo playerInfo;

}
