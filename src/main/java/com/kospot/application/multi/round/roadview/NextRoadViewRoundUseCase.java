package com.kospot.application.multi.round.roadview;

import com.kospot.application.multi.flow.MultiGameFlowScheduler;
import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.service.GamePlayerService;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.service.RoadViewGameRoundService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.multi.round.service.GameRoundNotificationService;
import com.kospot.infrastructure.websocket.domain.multi.timer.service.GameTimerService;
import com.kospot.infrastructure.websocket.domain.multi.timer.vo.TimerCommand;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class NextRoadViewRoundUseCase {

    private static final long INTRO_DURATION_MS = 8_000L;

    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GamePlayerService gamePlayerService;
    private final GameRoundNotificationService gameRoundNotificationService;
    private final GameTimerService gameTimerService;
    private final MultiGameFlowScheduler multiGameFlowScheduler;
    private final PlatformTransactionManager transactionManager;

    /**
     * 모든 플레이어 로딩이 끝난 직후 호출되어 1라운드를 준비한다.
     */
    public MultiRoadViewGameResponse.StartPlayerGame executeInitial(Long roomId, Long gameId) {
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(gameId);
        if (game.isInProgress()) {
            log.info("Game already started - skip initial execution. GameId: {}", gameId);
            return null;
        }
        game.startGame();

        List<GamePlayer> players = gamePlayerService.findPlayersByGameId(gameId);
        List<Long> playerIds = players.stream()
                .map(GamePlayer::getId)
                .toList();
        RoadViewGameRound round = roadViewGameRoundService.createGameRound(game, playerIds);

        MultiRoadViewGameResponse.StartPlayerGame preview =
                MultiRoadViewGameResponse.StartPlayerGame.from(game, round, players);

        scheduleRound(roomId.toString(), round, preview);
        return preview;
    }

    /**
     * 라운드가 종료된 뒤 다음 라운드를 준비할 때 사용한다.
     */
    public MultiRoadViewGameResponse.NextRound execute(Long roomId, Long gameId) {
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(gameId);
        game.moveToNextRound();

        RoadViewGameRound round = roadViewGameRoundService.createGameRound(game, null);
        MultiRoadViewGameResponse.NextRound preview = MultiRoadViewGameResponse.NextRound.from(game, round);

        scheduleRound(roomId.toString(), round, preview);
        return preview;
    }

    private void scheduleRound(String roomKey,
                               RoadViewGameRound round,
                               Object preview) {
        // 프리뷰를 즉시 브로드캐스트 해서 프론트가 8초간 오버레이를 재생할 수 있도록 한다.
        gameRoundNotificationService.broadcastRoundStart(roomKey, preview);

        multiGameFlowScheduler.schedule(roomKey, MultiGameFlowScheduler.FlowTaskType.INTRO,
                Duration.ofMillis(INTRO_DURATION_MS),
                () -> runInTransaction(() -> startRound(roomKey, round.getId())));
    }

    private void startRound(String roomKey, Long roundId) {
        RoadViewGameRound round = roadViewGameRoundService.getRound(roundId);
        // 8초 오버레이가 끝난 시점에 서버 기준 라운드 시작을 선언한다.
        round.startRound();
        gameTimerService.startRoundTimer(buildTimerCommand(roomKey, round));
    }

    private TimerCommand buildTimerCommand(String roomKey,
                                           RoadViewGameRound round) {
        MultiRoadViewGame game = round.getMultiRoadViewGame();
        return TimerCommand.builder()
                .round(round)
                .gameRoomId(roomKey)
                .gameId(game.getId())
                .gameMode(game.getGameMode())
                .matchType(game.getMatchType())
                .build();
    }

    private void runInTransaction(Runnable runnable) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.execute(status -> {
            runnable.run();
            return null;
        });
    }
}
