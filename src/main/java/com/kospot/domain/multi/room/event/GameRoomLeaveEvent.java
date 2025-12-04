package com.kospot.domain.multi.room.event;

import com.kospot.application.multi.room.vo.LeaveDecision;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.entity.GameRoom;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameRoomLeaveEvent {

    private final GameRoom gameRoom;
    private final Member leavingMember;
    private final LeaveDecision decision;

}
