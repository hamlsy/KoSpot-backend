package com.kospot.application.multi.round.roadview;

import com.kospot.application.multi.flow.MultiGameFlowScheduler;
import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.service.GamePlayerService;
import com.kospot.domain.multi.round.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.service.RoadViewGameRoundService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.exception.object.domain.GameRoundHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.redis.domain.multi.game.service.MultiGameRedisService;
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

    private final RoadViewGameRoundAdaptor roadViewGameRoundAdaptor;
    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GamePlayerService gamePlayerService;
    private final GameRoundNotificationService gameRoundNotificationService;
    private final GameTimerService gameTimerService;
    private final MultiGameFlowScheduler multiGameFlowScheduler;
    private final MultiGameRedisService multiGameRedisService;
    private final PlatformTransactionManager transactionManager;

    /**
     * 모든 플레이어 로딩이 끝난 직후 호출되어 1라운드를 준비한다.
     */
    public MultiRoadViewGameResponse.RoundPreview executeInitial(Long roomId, Long gameId) {
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

        String roomKey = roomId.toString();
        long version = multiGameRedisService.incrementRoundVersion(roomKey, round.getId());
        MultiRoadViewGameResponse.RoundPreview preview =
                MultiRoadViewGameResponse.RoundPreview.from(game, round, version);

        scheduleRound(roomKey, round, preview);
        return preview;
    }

    /**
     * 라운드가 종료된 뒤 다음 라운드를 준비할 때 사용한다.
     */
    public MultiRoadViewGameResponse.RoundPreview execute(Long roomId, Long gameId) {
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(gameId);
        game.moveToNextRound();

        RoadViewGameRound round = roadViewGameRoundService.createGameRound(game, null);
        String roomKey = roomId.toString();
        long version = multiGameRedisService.incrementRoundVersion(roomKey, round.getId());
        MultiRoadViewGameResponse.RoundPreview preview = MultiRoadViewGameResponse.RoundPreview.from(game, round, version);

        scheduleRound(roomKey, round, preview);
        return preview;
    }

    /**
     * 브라우저에서 좌표 로딩 실패 시 라운드 정보를 재발행한다. (1라운드 전용)
     */
    public MultiRoadViewGameResponse.RoundPreview reIssueInitial(Long roomId, Long roundId) {
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryByIdFetchGame(roundId);
        MultiRoadViewGame game = round.getMultiRoadViewGame();
        return reIssueInitial(roomId.toString(), round, game);
    }

    /**
     * 라운드 진행 중 좌표 재발행이 필요할 때 사용한다. (2라운드 이상)
     */
    public MultiRoadViewGameResponse.RoundPreview reIssue(Long roomId, Long roundId) {
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryByIdFetchGame(roundId);
        MultiRoadViewGame game = round.getMultiRoadViewGame();
        return reissueNext(roomId.toString(), round, game);
    }

    /**
     * 라운드 번호에 따라 적절한 재발행 로직을 선택한다.
     */
    public MultiRoadViewGameResponse.RoundPreview reissueRound(Long roomId, Long gameId, Long roundId) {
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryByIdFetchGame(roundId);
        MultiRoadViewGame game = round.getMultiRoadViewGame();
        if (!game.getId().equals(gameId)) {
            throw new GameRoundHandler(ErrorStatus.GAME_ROUND_NOT_FOUND);
        }
        String roomKey = roomId.toString();

        if (!multiGameRedisService.acquireRoundReissueLock(roomKey, roundId)) {
            long version = multiGameRedisService.getRoundVersion(roomKey, roundId);
            return MultiRoadViewGameResponse.RoundPreview.from(game, round, version);
        }
        try {
            if (round.getRoundNumber() <= 1) {
                return reIssueInitial(roomKey, round, game);
            }
            return reissueNext(roomKey, round, game);
        } finally {
            multiGameRedisService.releaseRoundReissueLock(roomKey, roundId);
        }
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
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryById(roundId);
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

    private MultiRoadViewGameResponse.RoundPreview reIssueInitial(String roomKey,
                                                                     RoadViewGameRound round,
                                                                     MultiRoadViewGame game) {
        roadViewGameRoundService.reissueRound(round, round.getPlayerIds());
        long version = multiGameRedisService.incrementRoundVersion(roomKey, round.getId());
        MultiRoadViewGameResponse.RoundPreview preview =
                MultiRoadViewGameResponse.RoundPreview.from(game, round, version);

        scheduleRound(roomKey, round, preview);
        log.info("Reissued initial round due to client reload request - RoomId: {}, RoundId: {}", roomKey, round.getId());
        return preview;
    }

    private MultiRoadViewGameResponse.RoundPreview reissueNext(String roomKey,
                                                            RoadViewGameRound round,
                                                            MultiRoadViewGame game) {
        roadViewGameRoundService.reissueRound(round, round.getPlayerIds());
        long version = multiGameRedisService.incrementRoundVersion(roomKey, round.getId());
        MultiRoadViewGameResponse.RoundPreview preview = MultiRoadViewGameResponse.RoundPreview.from(game, round, version);

        scheduleRound(roomKey, round, preview);
        log.info("Reissued round preview - RoomId: {}, RoundId: {}", roomKey, round.getId());
        return preview;
    }
}
