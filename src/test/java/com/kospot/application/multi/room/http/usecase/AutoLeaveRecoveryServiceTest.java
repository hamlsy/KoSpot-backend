package com.kospot.application.multi.room.http.usecase;

import com.kospot.common.exception.object.domain.GameRoomHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.multi.room.application.service.AutoLeaveRecoveryService;
import com.kospot.multi.room.application.usecase.LeaveGameRoomUseCase;
import com.kospot.multi.room.application.vo.LeaveRoomResult;
import com.kospot.multi.room.application.vo.ReconcileResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoLeaveRecoveryServiceTest {

    @Mock
    private LeaveGameRoomUseCase leaveGameRoomUseCase;

    @Mock
    private MemberAdaptor memberAdaptor;

    @InjectMocks
    private AutoLeaveRecoveryService autoLeaveRecoveryService;

    @Test
    @DisplayName("이전 방이 없으면 CLEAN으로 처리한다")
    void shouldReturnCleanWhenPreviousRoomIsNull() {
        ReconcileResult result = autoLeaveRecoveryService.reconcileBeforeTransition(1L, null, 20L, "JOIN");

        assertThat(result.getStatus()).isEqualTo(ReconcileResult.Status.CLEAN);
        assertThat(result.isFatalFailure()).isFalse();
    }

    @Test
    @DisplayName("leave 성공성 결과(ALREADY_LEFT)면 RECOVERED로 처리한다")
    void shouldReturnRecoveredOnAlreadyLeft() {
        when(leaveGameRoomUseCase.execute(1L, 10L))
                .thenReturn(LeaveRoomResult.alreadyLeft(1L, 10L, 10L, "already left"));

        ReconcileResult result = autoLeaveRecoveryService.reconcileBeforeTransition(1L, 10L, 20L, "JOIN");

        assertThat(result.getStatus()).isEqualTo(ReconcileResult.Status.RECOVERED);
        assertThat(result.getLeaveStatus()).isEqualTo(LeaveRoomResult.Status.ALREADY_LEFT);
    }

    @Test
    @DisplayName("retryable 예외면 멤버 roomId를 force detach 하고 RECOVERED 처리한다")
    void shouldForceDetachOnRecoverableException() {
        Member member = Member.builder()
                .id(1L)
                .gameRoomId(10L)
                .nickname("user")
                .username("user")
                .build();

        when(leaveGameRoomUseCase.execute(1L, 10L))
                .thenThrow(new GameRoomHandler(ErrorStatus.GAME_ROOM_OPERATION_IN_PROGRESS));
        when(memberAdaptor.queryById(1L)).thenReturn(member);

        ReconcileResult result = autoLeaveRecoveryService.reconcileBeforeTransition(1L, 10L, 20L, "JOIN");

        assertThat(result.getStatus()).isEqualTo(ReconcileResult.Status.RECOVERED);
        assertThat(member.getGameRoomId()).isNull();
        verify(memberAdaptor).queryById(1L);
    }

    @Test
    @DisplayName("fatal 예외면 FATAL_FAILURE 처리한다")
    void shouldReturnFatalOnNonRecoverableException() {
        when(leaveGameRoomUseCase.execute(1L, 10L))
                .thenThrow(new GameRoomHandler(ErrorStatus._INTERNAL_SERVER_ERROR));

        ReconcileResult result = autoLeaveRecoveryService.reconcileBeforeTransition(1L, 10L, 20L, "JOIN");

        assertThat(result.getStatus()).isEqualTo(ReconcileResult.Status.FATAL_FAILURE);
        assertThat(result.isFatalFailure()).isTrue();
    }
}
