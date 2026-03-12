package com.kospot.multi.room.application.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaveRoomResult {

    public enum Status {
        LEFT,
        ALREADY_LEFT,
        ROOM_NOT_FOUND_CLEANED,
        RETRYABLE_FAILURE,
        FATAL_FAILURE
    }

    private final Status status;
    private final Long memberId;
    private final Long requestedRoomId;
    private final Long effectiveRoomId;
    private final LeaveDecision.Action action;
    private final String message;

    public static LeaveRoomResult left(Long memberId, Long requestedRoomId, Long effectiveRoomId, LeaveDecision.Action action) {
        return LeaveRoomResult.builder()
                .status(Status.LEFT)
                .memberId(memberId)
                .requestedRoomId(requestedRoomId)
                .effectiveRoomId(effectiveRoomId)
                .action(action)
                .build();
    }

    public static LeaveRoomResult alreadyLeft(Long memberId, Long requestedRoomId, Long effectiveRoomId, String message) {
        return LeaveRoomResult.builder()
                .status(Status.ALREADY_LEFT)
                .memberId(memberId)
                .requestedRoomId(requestedRoomId)
                .effectiveRoomId(effectiveRoomId)
                .action(null)
                .message(message)
                .build();
    }

    public static LeaveRoomResult roomNotFoundCleaned(Long memberId, Long requestedRoomId, Long effectiveRoomId) {
        return LeaveRoomResult.builder()
                .status(Status.ROOM_NOT_FOUND_CLEANED)
                .memberId(memberId)
                .requestedRoomId(requestedRoomId)
                .effectiveRoomId(effectiveRoomId)
                .action(null)
                .message("Room not found; member state cleaned")
                .build();
    }

    public static LeaveRoomResult retryableFailure(Long memberId, Long requestedRoomId, Long effectiveRoomId, String message) {
        return LeaveRoomResult.builder()
                .status(Status.RETRYABLE_FAILURE)
                .memberId(memberId)
                .requestedRoomId(requestedRoomId)
                .effectiveRoomId(effectiveRoomId)
                .action(null)
                .message(message)
                .build();
    }

    public static LeaveRoomResult fatalFailure(Long memberId, Long requestedRoomId, Long effectiveRoomId, String message) {
        return LeaveRoomResult.builder()
                .status(Status.FATAL_FAILURE)
                .memberId(memberId)
                .requestedRoomId(requestedRoomId)
                .effectiveRoomId(effectiveRoomId)
                .action(null)
                .message(message)
                .build();
    }

    public boolean isSuccessLike() {
        return status == Status.LEFT
                || status == Status.ALREADY_LEFT
                || status == Status.ROOM_NOT_FOUND_CLEANED;
    }

    public boolean isFatalFailure() {
        return status == Status.FATAL_FAILURE;
    }
}
