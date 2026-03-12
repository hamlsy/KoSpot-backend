package com.kospot.common.lock.vo;

import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
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
        DELETE_ROOM, // 방 삭제 (마지막 플레이어)
        ALREADY_LEFT // 이미 퇴장됨(멱등)
    }

    public enum FailureType {
        NONE,
        RETRYABLE,
        FATAL
    }

    private final Action action;
    private final Long leavingMemberId;
    private final GameRoomPlayerInfo leavingPlayerInfo;
    private final GameRoomPlayerInfo newHostInfo; // CHANGE_HOST일 때만 사용
    private final boolean success;
    private final String errorMessage;
    @Builder.Default
    private final FailureType failureType = FailureType.NONE;

    public static HostAssignmentResult normalLeave(Long memberId, GameRoomPlayerInfo playerInfo) {
        return HostAssignmentResult.builder()
                .action(Action.NORMAL_LEAVE)
                .leavingMemberId(memberId)
                .leavingPlayerInfo(playerInfo)
                .success(true)
                .build();
    }

    public static HostAssignmentResult alreadyLeft(Long memberId) {
        return HostAssignmentResult.builder()
                .action(Action.ALREADY_LEFT)
                .leavingMemberId(memberId)
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

    public static HostAssignmentResult failureRetryable(String errorMessage) {
        return HostAssignmentResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .failureType(FailureType.RETRYABLE)
                .build();
    }

    public static HostAssignmentResult failureFatal(String errorMessage) {
        return HostAssignmentResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .failureType(FailureType.FATAL)
                .build();
    }

    public static HostAssignmentResult failure(String errorMessage) {
        return failureRetryable(errorMessage);
    }

    public boolean isRetryableFailure() {
        return !success && failureType == FailureType.RETRYABLE;
    }

    public boolean isFatalFailure() {
        return !success && failureType == FailureType.FATAL;
    }
}
