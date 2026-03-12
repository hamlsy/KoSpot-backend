package com.kospot.multi.room.application.service;

import com.kospot.common.exception.object.domain.GameRoomHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.multi.room.application.usecase.LeaveGameRoomUseCase;
import com.kospot.multi.room.application.vo.LeaveRoomResult;
import com.kospot.multi.room.application.vo.ReconcileResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoLeaveRecoveryService {

    private final LeaveGameRoomUseCase leaveGameRoomUseCase;
    private final MemberAdaptor memberAdaptor;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReconcileResult reconcileBeforeTransition(Long memberId, Long previousRoomId, Long targetRoomId, String source) {
        if (previousRoomId == null) {
            return ReconcileResult.clean(source, memberId, null, targetRoomId);
        }

        try {
            LeaveRoomResult leaveResult = leaveGameRoomUseCase.execute(memberId, previousRoomId);

            if (leaveResult.isSuccessLike()) {
                return ReconcileResult.recovered(
                        source,
                        memberId,
                        previousRoomId,
                        targetRoomId,
                        leaveResult.getStatus(),
                        leaveResult.getMessage());
            }

            if (leaveResult.isFatalFailure()) {
                return ReconcileResult.fatal(
                        source,
                        memberId,
                        previousRoomId,
                        targetRoomId,
                        leaveResult.getMessage());
            }

            forceDetachMemberRoom(memberId);
            return ReconcileResult.recovered(
                    source,
                    memberId,
                    previousRoomId,
                    targetRoomId,
                    leaveResult.getStatus(),
                    "Auto-leave retryable failure; force-detached member.gameRoomId");
        } catch (GameRoomHandler e) {
            if (isRecoverable(e)) {
                forceDetachMemberRoom(memberId);
                return ReconcileResult.recovered(
                        source,
                        memberId,
                        previousRoomId,
                        targetRoomId,
                        LeaveRoomResult.Status.RETRYABLE_FAILURE,
                        "Auto-leave exception recovered by force-detach");
            }

            log.error("Fatal auto-leave failure - Source: {}, MemberId: {}, PreviousRoomId: {}, TargetRoomId: {}",
                    source, memberId, previousRoomId, targetRoomId, e);
            return ReconcileResult.fatal(source, memberId, previousRoomId, targetRoomId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected auto-leave reconcile failure - Source: {}, MemberId: {}, PreviousRoomId: {}, TargetRoomId: {}",
                    source, memberId, previousRoomId, targetRoomId, e);
            return ReconcileResult.fatal(source, memberId, previousRoomId, targetRoomId, e.getMessage());
        }
    }

    private boolean isRecoverable(GameRoomHandler exception) {
        return ErrorStatus.GAME_ROOM_OPERATION_IN_PROGRESS.equals(exception.getCode())
                || ErrorStatus.GAME_ROOM_NOT_FOUND.equals(exception.getCode())
                || ErrorStatus.GAME_ROOM_PLAYER_NOT_FOUND.equals(exception.getCode());
    }

    private void forceDetachMemberRoom(Long memberId) {
        Member member = memberAdaptor.queryById(memberId);
        member.leaveGameRoom();
    }
}
