package com.kospot.application.multi.game.usecase;

import com.kospot.application.multi.round.roadview.NextRoadViewRoundUseCase;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartRoadViewSoloGameUseCaseTest {

    private static final long ROOM_ID = 11L;
    private static final long GAME_ID = 99L;

    @Mock
    private NextRoadViewRoundUseCase nextRoadViewRoundUseCase;

    @InjectMocks
    private StartRoadViewSoloGameUseCase startRoadViewSoloGameUseCase;

    @Test
    void execute_shouldDelegateToNextRoundUseCase() {
        MultiRoadViewGameResponse.StartPlayerGame preview = MultiRoadViewGameResponse.StartPlayerGame.builder()
                .gameId(GAME_ID)
                .currentRound(1)
                .totalRounds(5)
                .roundVersion(1L)
                .build();
        when(nextRoadViewRoundUseCase.executeInitial(ROOM_ID, GAME_ID)).thenReturn(preview);

        MultiRoadViewGameResponse.StartPlayerGame result =
                startRoadViewSoloGameUseCase.execute(ROOM_ID, GAME_ID);

        assertThat(result).isEqualTo(preview);
        verify(nextRoadViewRoundUseCase).executeInitial(ROOM_ID, GAME_ID);
    }

    @Test
    void execute_shouldReturnNullWhenInitialRoundAlreadyPrepared() {
        when(nextRoadViewRoundUseCase.executeInitial(ROOM_ID, GAME_ID)).thenReturn(null);

        MultiRoadViewGameResponse.StartPlayerGame result =
                startRoadViewSoloGameUseCase.execute(ROOM_ID, GAME_ID);

        assertNull(result);
        verify(nextRoadViewRoundUseCase).executeInitial(ROOM_ID, GAME_ID);
    }
}

