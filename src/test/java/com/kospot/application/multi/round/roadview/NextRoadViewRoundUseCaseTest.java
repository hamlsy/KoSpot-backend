package com.kospot.application.multi.round.roadview;

import com.kospot.application.multi.flow.MultiGameFlowScheduler;
import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.vo.MultiGameStatus;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.service.GamePlayerService;
import com.kospot.domain.multi.gamePlayer.vo.GamePlayerStatus;
import com.kospot.domain.multi.round.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.service.RoadViewGameRoundService;
import com.kospot.infrastructure.exception.object.domain.GameRoundHandler;
import com.kospot.infrastructure.redis.domain.multi.game.service.MultiGameRedisService;
import com.kospot.infrastructure.websocket.domain.multi.round.service.GameRoundNotificationService;
import com.kospot.infrastructure.websocket.domain.multi.timer.service.GameTimerService;
import com.kospot.infrastructure.websocket.domain.multi.timer.vo.TimerCommand;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NextRoadViewRoundUseCaseTest {

    private static final long ROOM_ID = 1L;
    private static final long GAME_ID = 10L;
    private static final long ROUND_ID = 100L;
    private static final Duration INTRO_DURATION = Duration.ofMillis(8_000L);

    @Mock
    private MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    @Mock
    private RoadViewGameRoundService roadViewGameRoundService;
    @Mock
    private GamePlayerService gamePlayerService;
    @Mock
    private GamePlayerAdaptor gamePlayerAdaptor;
    @Mock
    private GameRoundNotificationService gameRoundNotificationService;
    @Mock
    private GameTimerService gameTimerService;
    @Mock
    private MultiGameFlowScheduler multiGameFlowScheduler;
    @Mock
    private MultiGameRedisService multiGameRedisService;
    @Mock
    private RoadViewGameRoundAdaptor roadViewGameRoundAdaptor;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;
    @Captor
    private ArgumentCaptor<Duration> durationCaptor;
    @Captor
    private ArgumentCaptor<TimerCommand> timerCommandCaptor;
    @Captor
    private ArgumentCaptor<Object> previewCaptor;

    @InjectMocks
    private NextRoadViewRoundUseCase nextRoadViewRoundUseCase;

    @BeforeEach
    void setUp() {
        PlatformTransactionManager transactionManager = new ImmediateTransactionManager();
        nextRoadViewRoundUseCase = new NextRoadViewRoundUseCase(
                roadViewGameRoundAdaptor,
                multiRoadViewGameAdaptor,
                roadViewGameRoundService,
                gamePlayerAdaptor,
                gamePlayerService,
                gameRoundNotificationService,
                gameTimerService,
                multiGameFlowScheduler,
                multiGameRedisService,
                transactionManager
        );
    }

    @Test
    void executeInitial_shouldBroadcastPreviewAndScheduleTimer() {
        MultiRoadViewGame game = createPendingGame();
        List<GamePlayer> players = List.of(
                createPlayer(201L, "player1", "marker1"),
                createPlayer(202L, "player2", "marker2")
        );
        List<Long> playerIds = players.stream().map(GamePlayer::getId).toList();
        RoadViewGameRound round = createRound(game, 1, 37.0, 127.0, playerIds);

        when(multiRoadViewGameAdaptor.queryById(GAME_ID)).thenReturn(game);
        when(gamePlayerService.findPlayersByGameId(GAME_ID)).thenReturn(players);
        when(roadViewGameRoundService.createGameRound(eq(game), eq(playerIds))).thenReturn(round);
        when(multiGameRedisService.incrementRoundVersion(String.valueOf(ROOM_ID), ROUND_ID)).thenReturn(1L);
        when(roadViewGameRoundService.getRound(ROUND_ID)).thenReturn(round);

        long beforeRunRemaining = round.getRemainingTimeMs();

        MultiRoadViewGameResponse.StartPlayerGame response = nextRoadViewRoundUseCase.executeInitial(ROOM_ID, GAME_ID);

        assertThat(response).isNotNull();
        assertThat(response.getGameId()).isEqualTo(GAME_ID);
        assertThat(response.getRoundInfo().getRoundNumber()).isEqualTo(1);
        assertThat(response.getRoundInfo().getTargetLat()).isEqualTo(37.0);
        assertThat(response.getRoundInfo().getTargetLng()).isEqualTo(127.0);
        assertThat(response.getRoundVersion()).isEqualTo(1L);
        verify(roadViewGameRoundService).createGameRound(eq(game), eq(playerIds));
        verify(multiGameRedisService).incrementRoundVersion(String.valueOf(ROOM_ID), ROUND_ID);
        verify(gamePlayerService).findPlayersByGameId(GAME_ID);

        verify(gameRoundNotificationService).broadcastRoundStart(eq(String.valueOf(ROOM_ID)), previewCaptor.capture());
        assertThat(previewCaptor.getValue()).isEqualTo(response);

        verify(multiGameFlowScheduler).schedule(
                eq(String.valueOf(ROOM_ID)),
                eq(MultiGameFlowScheduler.FlowTaskType.INTRO),
                durationCaptor.capture(),
                runnableCaptor.capture()
        );
        assertThat(durationCaptor.getValue()).isEqualTo(INTRO_DURATION);

        runnableCaptor.getValue().run();

        long afterRunRemaining = round.getRemainingTimeMs();

        verify(roadViewGameRoundService).getRound(ROUND_ID);
        assertThat(afterRunRemaining).isLessThan(beforeRunRemaining);

        verify(gameTimerService).startRoundTimer(timerCommandCaptor.capture());
        TimerCommand command = timerCommandCaptor.getValue();
        assertThat(command.getRound()).isEqualTo(round);
        assertThat(command.getGameRoomId()).isEqualTo(String.valueOf(ROOM_ID));
        assertThat(command.getGameId()).isEqualTo(GAME_ID);
        assertThat(command.getGameMode()).isEqualTo(GameMode.ROADVIEW);
        assertThat(command.getMatchType()).isEqualTo(PlayerMatchType.SOLO);
    }

    @Test
    void executeInitial_shouldSkipWhenGameAlreadyStarted() {
        MultiRoadViewGame game = createInProgressGame();
        when(multiRoadViewGameAdaptor.queryById(GAME_ID)).thenReturn(game);

        MultiRoadViewGameResponse.StartPlayerGame response = nextRoadViewRoundUseCase.executeInitial(ROOM_ID, GAME_ID);

        assertNull(response);
        verifyNoInteractions(gamePlayerService);
        verifyNoInteractions(multiGameRedisService);
        verifyNoInteractions(roadViewGameRoundService);
        verifyNoInteractions(gameRoundNotificationService);
        verifyNoInteractions(multiGameFlowScheduler);
    }

    @Test
    void execute_shouldCreateNextRoundAndScheduleTimer() {
        MultiRoadViewGame game = createInProgressGame();
        RoadViewGameRound round = createRound(game, 2, 36.0, 128.0, List.of());

        when(multiRoadViewGameAdaptor.queryById(GAME_ID)).thenReturn(game);
        when(roadViewGameRoundService.createGameRound(eq(game), isNull())).thenReturn(round);
        when(multiGameRedisService.incrementRoundVersion(String.valueOf(ROOM_ID), ROUND_ID)).thenReturn(1L);
        when(roadViewGameRoundService.getRound(ROUND_ID)).thenReturn(round);

        MultiRoadViewGameResponse.NextRound response = nextRoadViewRoundUseCase.execute(ROOM_ID, GAME_ID);

        assertThat(response).isNotNull();
        assertThat(response.getGameId()).isEqualTo(GAME_ID);
        assertThat(response.getCurrentRound()).isEqualTo(2);
        assertThat(response.getRoundInfo().getTargetLat()).isEqualTo(36.0);
        assertThat(response.getRoundInfo().getTargetLng()).isEqualTo(128.0);
        assertThat(response.getRoundVersion()).isEqualTo(1L);
        verify(multiGameRedisService).incrementRoundVersion(String.valueOf(ROOM_ID), ROUND_ID);
        verifyNoInteractions(gamePlayerService);
        verify(roadViewGameRoundService).createGameRound(eq(game), isNull());

        verify(gameRoundNotificationService).broadcastRoundStart(eq(String.valueOf(ROOM_ID)), previewCaptor.capture());
        assertThat(previewCaptor.getValue()).isEqualTo(response);

        verify(multiGameFlowScheduler).schedule(
                eq(String.valueOf(ROOM_ID)),
                eq(MultiGameFlowScheduler.FlowTaskType.INTRO),
                durationCaptor.capture(),
                runnableCaptor.capture()
        );
        assertThat(durationCaptor.getValue()).isEqualTo(INTRO_DURATION);

        runnableCaptor.getValue().run();

        verify(gameTimerService).startRoundTimer(timerCommandCaptor.capture());
        assertThat(timerCommandCaptor.getValue().getRound()).isEqualTo(round);
    }

    @Test
    void reIssueInitial_shouldBroadcastNewPreviewAndReschedule() {
        MultiRoadViewGame game = createInProgressGame();
        List<GamePlayer> players = List.of(
                createPlayer(301L, "playerA", "markerA"),
                createPlayer(302L, "playerB", "markerB")
        );
        List<Long> playerIds = players.stream().map(GamePlayer::getId).toList();
        RoadViewGameRound round = createRound(game, 1, 37.5, 127.3, playerIds);
        Coordinate originalCoordinate = round.getTargetCoordinate();

        when(roadViewGameRoundService.getRound(ROUND_ID)).thenReturn(round);
        when(multiGameRedisService.acquireRoundReissueLock(String.valueOf(ROOM_ID), ROUND_ID)).thenReturn(true);
        when(roadViewGameRoundService.reissueRound(eq(round), eq(playerIds)))
                .thenAnswer(invocation -> {
                    round.reassignCoordinate(createCoordinate(38.0, 128.5));
                    return round;
                });
        when(multiGameRedisService.incrementRoundVersion(String.valueOf(ROOM_ID), ROUND_ID)).thenReturn(2L);

        MultiRoadViewGameResponse.RoundProblem response =
                nextRoadViewRoundUseCase.reissueInitial(ROOM_ID, ROUND_ID);

        assertThat(response).isNotNull();
        assertThat(round.getTargetCoordinate()).isNotEqualTo(originalCoordinate);
        assertThat(round.getTargetCoordinate().getLat()).isEqualTo(38.0);
        assertThat(round.getTargetCoordinate().getLng()).isEqualTo(128.5);
        assertThat(response.getRoundVersion()).isEqualTo(2L);

        verify(multiGameRedisService).acquireRoundReissueLock(String.valueOf(ROOM_ID), ROUND_ID);
        verify(roadViewGameRoundService).reissueRound(eq(round), eq(playerIds));
        verify(gameRoundNotificationService).broadcastRoundStart(eq(String.valueOf(ROOM_ID)),
                previewCaptor.capture());
        assertThat(previewCaptor.getValue()).isEqualTo(response);
        verifyNoInteractions(gamePlayerService);

        verify(multiGameFlowScheduler).schedule(
                eq(String.valueOf(ROOM_ID)),
                eq(MultiGameFlowScheduler.FlowTaskType.INTRO),
                durationCaptor.capture(),
                runnableCaptor.capture()
        );
        assertThat(durationCaptor.getValue()).isEqualTo(INTRO_DURATION);

        runnableCaptor.getValue().run();
        verify(gameTimerService).startRoundTimer(timerCommandCaptor.capture());
        assertThat(timerCommandCaptor.getValue().getRound()).isEqualTo(round);
        verify(multiGameRedisService).releaseRoundReissueLock(String.valueOf(ROOM_ID), ROUND_ID);
    }

    @Test
    void reIssue_shouldBroadcastNewPreviewForNextRounds() {
        MultiRoadViewGame game = createInProgressGame();
        game.moveToNextRound(); // 현재 라운드를 2로 설정
        List<Long> playerIds = List.of(401L, 402L);
        RoadViewGameRound round = createRound(game, 2, 36.2, 128.7, playerIds);
        Coordinate originalCoordinate = round.getTargetCoordinate();

        when(roadViewGameRoundService.getRound(ROUND_ID)).thenReturn(round);
        when(multiGameRedisService.acquireRoundReissueLock(String.valueOf(ROOM_ID), ROUND_ID)).thenReturn(true);
        when(roadViewGameRoundService.reissueRound(eq(round), eq(playerIds)))
                .thenAnswer(invocation -> {
                    round.reassignCoordinate(createCoordinate(36.8, 129.1));
                    return round;
                });
        when(multiGameRedisService.incrementRoundVersion(String.valueOf(ROOM_ID), ROUND_ID)).thenReturn(4L);

        MultiRoadViewGameResponse.RoundProblem response =
                nextRoadViewRoundUseCase.reissue(ROOM_ID, ROUND_ID);

        assertThat(response).isNotNull();
        assertThat(round.getTargetCoordinate()).isNotEqualTo(originalCoordinate);
        assertThat(round.getTargetCoordinate().getLat()).isEqualTo(36.8);
        assertThat(round.getTargetCoordinate().getLng()).isEqualTo(129.1);
        assertThat(response.getRoundVersion()).isEqualTo(4L);

        verify(multiGameRedisService).acquireRoundReissueLock(String.valueOf(ROOM_ID), ROUND_ID);
        verifyNoInteractions(gamePlayerService);
        verify(roadViewGameRoundService).reissueRound(eq(round), eq(playerIds));
        verify(gameRoundNotificationService).broadcastRoundStart(eq(String.valueOf(ROOM_ID)),
                previewCaptor.capture());
        assertThat(previewCaptor.getValue()).isEqualTo(response);

        verify(multiGameFlowScheduler).schedule(
                eq(String.valueOf(ROOM_ID)),
                eq(MultiGameFlowScheduler.FlowTaskType.INTRO),
                durationCaptor.capture(),
                runnableCaptor.capture()
        );
        assertThat(durationCaptor.getValue()).isEqualTo(INTRO_DURATION);

        runnableCaptor.getValue().run();
        verify(gameTimerService).startRoundTimer(timerCommandCaptor.capture());
        assertThat(timerCommandCaptor.getValue().getRound()).isEqualTo(round);
        verify(multiGameRedisService).releaseRoundReissueLock(String.valueOf(ROOM_ID), ROUND_ID);
    }

    @Test
    void reIssueRound_shouldThrowWhenGameIdMismatch() {
        MultiRoadViewGame game = createInProgressGame();
        List<Long> playerIds = List.of(501L);
        RoadViewGameRound round = createRound(game, 1, 37.9, 127.9, playerIds);

        when(roadViewGameRoundService.getRound(ROUND_ID)).thenReturn(round);

        assertThatThrownBy(() -> nextRoadViewRoundUseCase.reissueRound(ROOM_ID, GAME_ID + 1, ROUND_ID))
                .isInstanceOf(GameRoundHandler.class);
    }

    @Test
    void reIssueRound_shouldReturnExistingPreviewWhenLockNotAcquired_initialRound() {
        MultiRoadViewGame game = createInProgressGame();
        List<GamePlayer> players = List.of(createPlayer(601L, "playerX", "markerX"));
        List<Long> playerIds = players.stream().map(GamePlayer::getId).toList();
        RoadViewGameRound round = createRound(game, 1, 37.1, 127.1, playerIds);

        when(roadViewGameRoundService.getRound(ROUND_ID)).thenReturn(round);
        when(multiGameRedisService.acquireRoundReissueLock(String.valueOf(ROOM_ID), ROUND_ID)).thenReturn(false);
        when(multiGameRedisService.getRoundVersion(String.valueOf(ROOM_ID), ROUND_ID)).thenReturn(5L);

        MultiRoadViewGameResponse.RoundProblem response =
                nextRoadViewRoundUseCase.reissueRound(ROOM_ID, GAME_ID, ROUND_ID);

        assertThat(response.getRoundVersion()).isEqualTo(5L);
        verify(multiGameRedisService).acquireRoundReissueLock(String.valueOf(ROOM_ID), ROUND_ID);
        verifyNoInteractions(gamePlayerService);
        verify(multiGameFlowScheduler, never()).schedule(anyString(), any(), any(), any());
        verifyNoInteractions(gameTimerService);
        verify(multiGameRedisService, never()).releaseRoundReissueLock(anyString(), anyLong());
    }

    @Test
    void reIssueRound_shouldReturnExistingPreviewWhenLockNotAcquired_nextRound() {
        MultiRoadViewGame game = createInProgressGame();
        game.moveToNextRound();
        List<Long> playerIds = List.of(701L, 702L);
        RoadViewGameRound round = createRound(game, 2, 35.5, 129.5, playerIds);

        when(roadViewGameRoundService.getRound(ROUND_ID)).thenReturn(round);
        when(multiGameRedisService.acquireRoundReissueLock(String.valueOf(ROOM_ID), ROUND_ID)).thenReturn(false);
        when(multiGameRedisService.getRoundVersion(String.valueOf(ROOM_ID), ROUND_ID)).thenReturn(7L);

        MultiRoadViewGameResponse.RoundProblem response =
                nextRoadViewRoundUseCase.reissueRound(ROOM_ID, GAME_ID, ROUND_ID);

        assertThat(response.getRoundVersion()).isEqualTo(7L);
        verify(multiGameRedisService).acquireRoundReissueLock(String.valueOf(ROOM_ID), ROUND_ID);
        verify(multiGameFlowScheduler, never()).schedule(anyString(), any(), any(), any());
        verifyNoInteractions(gameTimerService);
        verify(multiGameRedisService, never()).releaseRoundReissueLock(anyString(), anyLong());
    }

    private MultiRoadViewGame createPendingGame() {
        return MultiRoadViewGame.builder()
                .id(GAME_ID)
                .title("pending-game")
                .matchType(PlayerMatchType.SOLO)
                .gameMode(GameMode.ROADVIEW)
                .timeLimit(60)
                .totalRounds(5)
                .currentRound(0)
                .isFinished(false)
                .status(MultiGameStatus.PENDING)
                .build();
    }

    private MultiRoadViewGame createInProgressGame() {
        return MultiRoadViewGame.builder()
                .id(GAME_ID)
                .title("progress-game")
                .matchType(PlayerMatchType.SOLO)
                .gameMode(GameMode.ROADVIEW)
                .timeLimit(60)
                .totalRounds(5)
                .currentRound(1)
                .isFinished(false)
                .status(MultiGameStatus.IN_PROGRESS)
                .build();
    }

    private RoadViewGameRound createRound(MultiRoadViewGame game,
                                          int roundNumber,
                                          double lat,
                                          double lng,
                                          List<Long> playerIds) {
        Coordinate coordinate = Coordinate.builder()
                .id(900L + roundNumber)
                .lat(lat)
                .lng(lng)
                .poiName("poi-" + roundNumber)
                .build();

        return RoadViewGameRound.builder()
                .id(ROUND_ID)
                .multiRoadViewGame(game)
                .roundNumber(roundNumber)
                .targetCoordinate(coordinate)
                .timeLimit(60)
                .playerIds(playerIds)
                .build();
    }

    private GamePlayer createPlayer(Long id, String nickname, String markerUrl) {
        return GamePlayer.builder()
                .id(id)
                .nickname(nickname)
                .equippedMarkerImageUrl(markerUrl)
                .totalScore(0.0)
                .roundRank(1)
                .status(GamePlayerStatus.PLAYING)
                .build();
    }

    private Coordinate createCoordinate(double lat, double lng) {
        return Coordinate.builder()
                .id((long) (lat * 1000))
                .lat(lat)
                .lng(lng)
                .poiName("reissue-poi")
                .build();
    }

    private static class ImmediateTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            // no-op
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            // no-op
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            // no-op
        }
    }
}

