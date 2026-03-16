package com.kospot.application.multi.round.roadview;

import com.kospot.multi.common.flow.MultiGameFlowScheduler;
import com.kospot.multi.game.application.service.CancelMultiGameService;
import com.kospot.coordinate.domain.entity.Coordinate;
import com.kospot.multi.game.domain.entity.MultiRoadViewGame;
import com.kospot.multi.round.application.service.roadview.RoundPreparationService;
import com.kospot.multi.round.entity.RoadViewGameRound;
import com.kospot.multi.lobby.infrastructure.websocket.service.LobbyRoomNotificationService;
import com.kospot.multi.round.infrastructure.websocket.service.GameRoundNotificationService;
import com.kospot.multi.timer.infrastructure.websocket.service.GameTimerService;
import com.kospot.multi.game.presentation.dto.response.MultiRoadViewGameResponse;
import com.kospot.multi.round.application.usecase.roadview.NextRoadViewRoundUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NextRoadViewRoundUseCaseTest {

    private static final Long ROOM_ID = 1L;
    private static final Long GAME_ID = 2L;
    private static final Long ROUND_ID = 3L;

    @Mock
    private RoundPreparationService roundPreparationService;

    @Mock
    private GameRoundNotificationService gameRoundNotificationService;

    @Mock
    private LobbyRoomNotificationService lobbyRoomNotificationService;

    @Mock
    private MultiGameFlowScheduler multiGameFlowScheduler;

    @Mock
    private GameTimerService gameTimerService;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private CancelMultiGameService cancelMultiGameService;

    @InjectMocks
    private NextRoadViewRoundUseCase useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "maxReissueCountPerRound", 5);
        ReflectionTestUtils.setField(useCase, "reissueCooldownMs", 1_000L);
    }

    @Test
    void reissueRound_returnsLatestWithoutMutation_whenCasRejected() {
        MultiRoadViewGame game = mockGame();
        RoadViewGameRound round = mockRound(game);

        when(roundPreparationService.tryAdvanceReissueVersion(
                eq(ROUND_ID), eq(GAME_ID), eq(10L), anyInt(), any(), any())).thenReturn(0);
        when(roundPreparationService.getRoundForReissue(ROUND_ID, GAME_ID, ROOM_ID)).thenReturn(round);
        when(round.getRoundVersion()).thenReturn(11L);

        MultiRoadViewGameResponse.RoundProblem response = useCase.reissueRound(ROOM_ID, GAME_ID, ROUND_ID, 10L);

        assertThat(response.getRoundVersion()).isEqualTo(11L);
        assertThat(response.getGameId()).isEqualTo(GAME_ID);
        assertThat(response.getRoundId()).isEqualTo(ROUND_ID);
        assertThat(response.isReissued()).isFalse();

        verify(roundPreparationService, never()).reissueRound(any(RoadViewGameRound.class), eq(GAME_ID));
        verify(gameRoundNotificationService, never()).broadcastRoundStart(eq("1"), any());
    }

    @Test
    void reissueRound_reissuesAndBroadcasts_whenCasAccepted() {
        MultiRoadViewGame game = mockGame();
        RoadViewGameRound round = mockRound(game);

        when(roundPreparationService.tryAdvanceReissueVersion(
                eq(ROUND_ID), eq(GAME_ID), eq(20L), anyInt(), any(), any())).thenReturn(1);
        when(roundPreparationService.getRoundForReissueWithLock(ROUND_ID, GAME_ID)).thenReturn(round);
        when(roundPreparationService.reissueRound(same(round), eq(GAME_ID)))
                .thenReturn(new RoundPreparationService.ReissueResult(game, round));
        when(round.getRoundVersion()).thenReturn(21L);

        MultiRoadViewGameResponse.RoundProblem response = useCase.reissueRound(ROOM_ID, GAME_ID, ROUND_ID, 20L);

        assertThat(response.getRoundVersion()).isEqualTo(21L);
        assertThat(response.getGameId()).isEqualTo(GAME_ID);
        assertThat(response.getRoundId()).isEqualTo(ROUND_ID);
        assertThat(response.isReissued()).isTrue();

        verify(roundPreparationService, times(1)).validateOwnership(same(round), eq(GAME_ID), eq(ROOM_ID));
        verify(roundPreparationService, times(1)).reissueRound(same(round), eq(GAME_ID));
        verify(gameRoundNotificationService, times(1)).broadcastRoundStart(eq("1"), any());
    }

    private MultiRoadViewGame mockGame() {
        MultiRoadViewGame game = org.mockito.Mockito.mock(MultiRoadViewGame.class);
        when(game.getId()).thenReturn(GAME_ID);
        when(game.isPoiNameVisible()).thenReturn(true);
        return game;
    }

    private RoadViewGameRound mockRound(MultiRoadViewGame game) {
        RoadViewGameRound round = org.mockito.Mockito.mock(RoadViewGameRound.class);
        Coordinate coordinate = org.mockito.Mockito.mock(Coordinate.class);
        when(round.getId()).thenReturn(ROUND_ID);
        when(round.getMultiRoadViewGame()).thenReturn(game);
        when(round.getTargetCoordinate()).thenReturn(coordinate);
        when(coordinate.getPoiName()).thenReturn("poi");
        when(coordinate.getLat()).thenReturn(37.0);
        when(coordinate.getLng()).thenReturn(127.0);
        return round;
    }
}
