package com.kospot.common.lock.vo;

import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import lombok.Builder;
import lombok.Getter;

/**
 * 방장 재지정 작업의 결과를 담는 VO
 */
@Getter
@Builder
public class HostAssignmentResult {

    public enum Action {
        NORMAL_LEAVE, // 일반 플레이어 퇴장
        CHANGE_HOST, // 방장 변경
        DELETE_ROOM // 방 삭제 (마지막 플레이어)
    }

    private final Action action;
    private final Long leavingMemberId;
    private final GameRoomPlayerInfo leavingPlayerInfo;
    private final GameRoomPlayerInfo newHostInfo; // CHANGE_HOST일 때만 사용
    private final boolean success;
    private final String errorMessage;

    public static HostAssignmentResult normalLeave(Long memberId, GameRoomPlayerInfo playerInfo) {
        return HostAssignmentResult.builder()
                .action(Action.NORMAL_LEAVE)
                .leavingMemberId(memberId)
                .leavingPlayerInfo(playerInfo)
                .success(true)
                .build();
    }

    public static HostAssignmentResult changeHost(Long memberId, GameRoomPlayerInfo leavingPlayer,
            GameRoomPlayerInfo newHost) {
        return HostAssignmentResult.builder()
                .action(Action.CHANGE_HOST)
                .leavingMemberId(memberId)
                .leavingPlayerInfo(leavingPlayer)
                .newHostInfo(newHost)
                .success(true)
                .build();
    }

    public static HostAssignmentResult deleteRoom(Long memberId, GameRoomPlayerInfo playerInfo) {
        return HostAssignmentResult.builder()
                .action(Action.DELETE_ROOM)
                .leavingMemberId(memberId)
                .leavingPlayerInfo(playerInfo)
                .success(true)
                .build();
    }

    public static HostAssignmentResult failure(String errorMessage) {
        return HostAssignmentResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
