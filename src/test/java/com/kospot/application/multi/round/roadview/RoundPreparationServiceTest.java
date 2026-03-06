package com.kospot.application.multi.round.roadview;

import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.service.roadview.RoadViewGameRoundService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundPreparationServiceTest {

    private static final Long GAME_ID = 10L;

    @Mock
    private MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;

    @Mock
    private RoadViewGameRoundAdaptor roadViewGameRoundAdaptor;

    @Mock
    private RoadViewGameRoundService roadViewGameRoundService;

    @Mock
    private GamePlayerAdaptor gamePlayerAdaptor;

    @InjectMocks
    private RoundPreparationService roundPreparationService;

    @Test
    void prepareInitialRound_returnsNull_whenGameAlreadyStartedOrNotPending() {
        when(multiRoadViewGameAdaptor.transitionToInProgressIfPending(GAME_ID)).thenReturn(false);

        RoundPreparationService.InitialRoundResult result = roundPreparationService.prepareInitialRound(GAME_ID);

        assertThat(result).isNull();
        verify(multiRoadViewGameAdaptor, never()).queryById(GAME_ID);
        verifyNoInteractions(roadViewGameRoundService);
    }

    @Test
    void prepareInitialRound_createsFirstRound_whenTransitionSucceeded() {
        MultiRoadViewGame game = mock(MultiRoadViewGame.class);
        GamePlayer player1 = mock(GamePlayer.class);
        GamePlayer player2 = mock(GamePlayer.class);
        RoadViewGameRound round = mock(RoadViewGameRound.class);

        when(multiRoadViewGameAdaptor.transitionToInProgressIfPending(GAME_ID)).thenReturn(true);
        when(multiRoadViewGameAdaptor.queryById(GAME_ID)).thenReturn(game);
        when(gamePlayerAdaptor.queryByMultiRoadViewGameIdWithMember(GAME_ID)).thenReturn(List.of(player1, player2));
        when(player1.getId()).thenReturn(1L);
        when(player2.getId()).thenReturn(2L);
        when(roadViewGameRoundService.createGameRound(game, List.of(1L, 2L))).thenReturn(round);

        RoundPreparationService.InitialRoundResult result = roundPreparationService.prepareInitialRound(GAME_ID);

        assertThat(result).isNotNull();
        assertThat(result.getGame()).isSameAs(game);
        assertThat(result.getRound()).isSameAs(round);
        assertThat(result.getPlayers()).containsExactly(player1, player2);
        verify(multiRoadViewGameAdaptor).queryById(GAME_ID);
        verify(roadViewGameRoundService).createGameRound(game, List.of(1L, 2L));
    }
}
