package com.kospot.multi.room.application.service;

import com.kospot.common.exception.object.domain.GameRoomHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.multi.room.application.usecase.LeaveGameRoomUseCase;
import com.kospot.multi.room.application.vo.LeaveRoomResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomExitOrchestrator {

    private final LeaveGameRoomUseCase leaveGameRoomUseCase;

    public LeaveRoomResult requestExit(Long memberId, Long gameRoomId, String source, String reason) {
        if (memberId == null || memberId <= 0 || gameRoomId == null) {
            return LeaveRoomResult.alreadyLeft(memberId, gameRoomId, gameRoomId,
                    "Skip leave because memberId or gameRoomId is null");
        }

        try {
            LeaveRoomResult result = leaveGameRoomUseCase.execute(memberId, gameRoomId);
            log.info("Room exit requested - Source: {}, Reason: {}, MemberId: {}, RoomId: {}, Status: {}",
                    source, reason, memberId, gameRoomId, result.getStatus());
            return result;
        } catch (GameRoomHandler e) {
            if (ErrorStatus.GAME_ROOM_OPERATION_IN_PROGRESS.equals(e.getCode())) {
                log.warn("Retryable room exit failure - Source: {}, Reason: {}, MemberId: {}, RoomId: {}",
                        source, reason, memberId, gameRoomId, e);
                return LeaveRoomResult.retryableFailure(memberId, gameRoomId, gameRoomId, e.getMessage());
            }

            log.error("Fatal room exit failure - Source: {}, Reason: {}, MemberId: {}, RoomId: {}",
                    source, reason, memberId, gameRoomId, e);
            return LeaveRoomResult.fatalFailure(memberId, gameRoomId, gameRoomId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected room exit failure - Source: {}, Reason: {}, MemberId: {}, RoomId: {}",
                    source, reason, memberId, gameRoomId, e);
            return LeaveRoomResult.fatalFailure(memberId, gameRoomId, gameRoomId, e.getMessage());
        }
    }
}
