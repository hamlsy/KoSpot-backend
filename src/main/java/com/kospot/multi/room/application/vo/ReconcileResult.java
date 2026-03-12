package com.kospot.multi.room.application.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReconcileResult {

    public enum Status {
        CLEAN,
        RECOVERED,
        FATAL_FAILURE
    }

    private final Status status;
    private final String source;
    private final Long memberId;
    private final Long previousRoomId;
    private final Long targetRoomId;
    private final LeaveRoomResult.Status leaveStatus;
    private final String message;

    public static ReconcileResult clean(String source, Long memberId, Long previousRoomId, Long targetRoomId) {
        return ReconcileResult.builder()
                .status(Status.CLEAN)
                .source(source)
                .memberId(memberId)
                .previousRoomId(previousRoomId)
                .targetRoomId(targetRoomId)
                .leaveStatus(null)
                .message(null)
                .build();
    }

    public static ReconcileResult recovered(String source, Long memberId, Long previousRoomId, Long targetRoomId,
            LeaveRoomResult.Status leaveStatus, String message) {
        return ReconcileResult.builder()
                .status(Status.RECOVERED)
                .source(source)
                .memberId(memberId)
                .previousRoomId(previousRoomId)
                .targetRoomId(targetRoomId)
                .leaveStatus(leaveStatus)
                .message(message)
                .build();
    }

    public static ReconcileResult fatal(String source, Long memberId, Long previousRoomId, Long targetRoomId,
            String message) {
        return ReconcileResult.builder()
                .status(Status.FATAL_FAILURE)
                .source(source)
                .memberId(memberId)
                .previousRoomId(previousRoomId)
                .targetRoomId(targetRoomId)
                .leaveStatus(LeaveRoomResult.Status.FATAL_FAILURE)
                .message(message)
                .build();
    }

    public boolean isFatalFailure() {
        return status == Status.FATAL_FAILURE;
    }
}
