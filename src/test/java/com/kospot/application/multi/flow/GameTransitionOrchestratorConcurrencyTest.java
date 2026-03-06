package com.kospot.application.multi.flow;

import com.kospot.application.multi.game.message.LoadingStatusMessage;
import com.kospot.application.multi.round.roadview.NextRoadViewRoundUseCase;
import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.presentation.multi.game.dto.message.LoadingAckMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameTransitionOrchestratorConcurrencyTest {

    private static final String ROOM_ID = "1";
    private static final Long GAME_ID = 2L;

    @Mock
    private LoadingPhaseService loadingPhaseService;

    @Mock
    private MultiGameFlowScheduler multiGameFlowScheduler;

    @Mock
    private NextRoadViewRoundUseCase nextRoadViewRoundUseCase;

    @Mock
    private MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;

    @InjectMocks
    private GameTransitionOrchestrator orchestrator;

    @Test
    void handleLoadingAck_startsOnlyOnce_whenTwoAcksArriveConcurrently() throws Exception {
        LoadingStatusMessage allArrived = LoadingStatusMessage.builder()
                .players(java.util.List.of())
                .allArrived(true)
                .build();

        when(loadingPhaseService.buildLoadingStatusMessage(ROOM_ID)).thenReturn(allArrived);
        when(loadingPhaseService.getCurrentGameId(ROOM_ID)).thenReturn(GAME_ID);

        AtomicBoolean winner = new AtomicBoolean(false);
        when(loadingPhaseService.acquireGameStartLock(ROOM_ID, GAME_ID))
                .thenAnswer(invocation -> winner.compareAndSet(false, true));

        CountDownLatch bothReachedMarkReady = new CountDownLatch(2);
        CountDownLatch releaseMarkReady = new CountDownLatch(1);

        doAnswer(invocation -> {
            bothReachedMarkReady.countDown();
            boolean released = releaseMarkReady.await(2, TimeUnit.SECONDS);
            assertThat(released).isTrue();
            return null;
        }).when(loadingPhaseService).markPlayerReady(eq(ROOM_ID), anyLong(), anyLong(), anyLong());

        LoadingAckMessage message = new LoadingAckMessage();
        message.setRoundId(0L);
        message.setClientTimestamp(System.currentTimeMillis());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> orchestrator.handleLoadingAck(
                    ROOM_ID, message, headerAccessor(101L)));
            Future<?> second = executor.submit(() -> orchestrator.handleLoadingAck(
                    ROOM_ID, message, headerAccessor(102L)));

            boolean bothWaiting = bothReachedMarkReady.await(2, TimeUnit.SECONDS);
            assertThat(bothWaiting).isTrue();
            releaseMarkReady.countDown();

            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        verify(loadingPhaseService, times(2)).acquireGameStartLock(ROOM_ID, GAME_ID);
        verify(nextRoadViewRoundUseCase, times(1)).executeInitial(1L, GAME_ID);
        verify(loadingPhaseService, times(1)).cleanupLoadingState(ROOM_ID);
        verify(multiGameFlowScheduler, times(1))
                .cancel(ROOM_ID, MultiGameFlowScheduler.FlowTaskType.LOADING_TIMEOUT);
    }

    private SimpMessageHeaderAccessor headerAccessor(Long memberId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        HashMap<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("user", new WebSocketMemberPrincipal(memberId, "nick", "n@k.com", "USER"));
        accessor.setSessionAttributes(sessionAttributes);
        return accessor;
    }
}
