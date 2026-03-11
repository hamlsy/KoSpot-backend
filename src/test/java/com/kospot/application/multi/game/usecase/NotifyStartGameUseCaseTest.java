package com.kospot.application.multi.game.usecase;

import com.kospot.common.exception.object.domain.GameHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.multi.common.flow.GameTransitionOrchestrator;
import com.kospot.multi.game.application.service.GameStartReadinessService;
import com.kospot.multi.game.application.strategy.MultiGameStartStrategy;
import com.kospot.multi.game.application.usecase.NotifyStartGameUseCase;
import com.kospot.multi.game.infrastructure.websocket.service.GameNotificationService;
import com.kospot.multi.room.application.adaptor.GameRoomAdaptor;
import com.kospot.multi.room.application.service.service.GameRoomService;
import com.kospot.multi.room.domain.entity.GameRoom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyStartGameUseCaseTest {

    @Mock
    private MemberAdaptor memberAdaptor;

    @Mock
    private GameRoomService gameRoomService;

    @Mock
    private GameRoomAdaptor gameRoomAdaptor;

    @Mock
    private GameStartReadinessService gameStartReadinessService;

    @Mock
    private GameTransitionOrchestrator gameTransitionOrchestrator;

    @Mock
    private GameNotificationService gameNotificationService;

    @Mock
    private MultiGameStartStrategy startStrategy;

    @Test
    @DisplayName("시작 전 ROOM 준비가 안 되었으면 게임 시작을 차단한다")
    void execute_shouldRejectStart_whenPlayersNotReady() {
        NotifyStartGameUseCase useCase = new NotifyStartGameUseCase(
                memberAdaptor,
                gameRoomService,
                gameRoomAdaptor,
                gameStartReadinessService,
                List.of(startStrategy),
                gameTransitionOrchestrator,
                gameNotificationService
        );

        Long hostId = 1L;
        Long gameRoomId = 20L;
        Member host = org.mockito.Mockito.mock(Member.class);
        GameRoom gameRoom = org.mockito.Mockito.mock(GameRoom.class);

        when(memberAdaptor.queryById(hostId)).thenReturn(host);
        when(gameRoomAdaptor.queryByIdFetchHost(gameRoomId)).thenReturn(gameRoom);
        doThrow(new GameHandler(ErrorStatus._BAD_REQUEST))
                .when(gameStartReadinessService)
                .validateReadyToStart(gameRoomId.toString());

        assertThrows(GameHandler.class, () -> useCase.execute(hostId, gameRoomId));

        verify(gameRoomService, never()).markGameRoomAsInGame(gameRoom, host);
        verify(gameNotificationService, never()).broadcastGameStart(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        verify(gameTransitionOrchestrator, never()).initializeLoadingPhase(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
    }
}
