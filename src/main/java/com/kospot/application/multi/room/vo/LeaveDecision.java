package com.kospot.application.multi.room.vo;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaveDecision {

    public enum Action {
        NORMAL_LEAVE,
        CHANGE_HOST,
        DELETE_ROOM
    }

    private final Action action;
    private final GameRoom gameRoom;
    private final Member leavingMember;
    private final GameRoomPlayerInfo newHostInfo;

    public static LeaveDecision normalLeave(Member member) {
        return LeaveDecision.builder()
                .action(Action.NORMAL_LEAVE)
                .leavingMember(member)
                .build();
    }

    public static LeaveDecision deleteRoom(GameRoom gameRoom, Member leavingHost) {
        return LeaveDecision.builder()
                .action(Action.DELETE_ROOM)
                .gameRoom(gameRoom)
                .leavingMember(leavingHost)
                .build();
    }

    public static LeaveDecision changeHost(
            GameRoom gameRoom,
            Member leavingHost,
            GameRoomPlayerInfo newHostInfo
    ) {
        return LeaveDecision.builder()
                .action(Action.CHANGE_HOST)
                .gameRoom(gameRoom)
                .leavingMember(leavingHost)
                .newHostInfo(newHostInfo)
                .build();
    }

    public boolean isHostChange() {
        return action == Action.CHANGE_HOST;
    }

    public boolean isRoomDeletion() {
        return action == Action.DELETE_ROOM;
    }

}
