package com.kospot.multi.round.application.usecase.roadview;

import com.kospot.multi.common.flow.MultiGameFlowScheduler;
import com.kospot.common.exception.object.domain.GameRoundHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.multi.game.application.service.CancelMultiGameService;
import com.kospot.multi.game.domain.entity.MultiRoadViewGame;
import com.kospot.multi.room.domain.vo.GameRoomStatus;
import com.kospot.multi.round.application.service.roadview.RoundPreparationService;
import com.kospot.multi.round.entity.RoadViewGameRound;
import com.kospot.common.annotation.usecase.UseCase;

import com.kospot.multi.lobby.infrastructure.websocket.service.LobbyRoomNotificationService;
import com.kospot.multi.round.infrastructure.websocket.service.GameRoundNotificationService;
import com.kospot.multi.timer.infrastructure.websocket.service.GameTimerService;
import com.kospot.multi.timer.entity.vo.TimerCommand;
import com.kospot.multi.game.presentation.dto.response.MultiRoadViewGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Instant;

/**
 * 로드뷰 라운드 진행을 조율하는 UseCase
 * 도메인 로직은 RoundPreparationService에 위임한다.
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class NextRoadViewRoundUseCase {

    private static final long INTRO_DURATION_MS = 8_000L;

    // Domain Service
    private final RoundPreparationService roundPreparationService;

    // Infrastructure Services (직접 사용)
    private final GameRoundNotificationService gameRoundNotificationService;
    private final LobbyRoomNotificationService lobbyRoomNotificationService;
    private final MultiGameFlowScheduler multiGameFlowScheduler;
    private final GameTimerService gameTimerService;
    private final PlatformTransactionManager transactionManager;
    private final CancelMultiGameService cancelMultiGameService;

    @Value("${multi.roadview.reissue.max-count-per-round:5}")
    private Integer maxReissueCountPerRound;

    @Value("${multi.roadview.reissue.cooldown-ms:1000}")
    private Long reissueCooldownMs;

    /**
     * 모든 플레이어 로딩이 끝난 직후 호출되어 1라운드를 준비한다.
     */
    public MultiRoadViewGameResponse.StartPlayerGame executeInitial(Long roomId, Long gameId) {
        // 활성 플레이어 검증 (안전장치)
        if (cancelMultiGameService.cancelIfNoActivePlayers(roomId, gameId)) {
            log.info("Game cancelled before initial round - no active players. RoomId: {}, GameId: {}", roomId, gameId);
            return null;
        }

        // 도메인 서비스에 라운드 준비 위임
        RoundPreparationService.InitialRoundResult result =
                roundPreparationService.prepareInitialRound(gameId);
        if (result == null) {
            log.info("Initial round already prepared - RoomId: {}, GameId: {}", roomId, gameId);
            return null;
        }

        String roomKey = roomId.toString();
        long version = result.getRound().getRoundVersion();

        MultiRoadViewGameResponse.StartPlayerGame preview =
                MultiRoadViewGameResponse.StartPlayerGame.from(
                        result.getGame(), result.getRound(), result.getPlayers(), version);

        // 라운드 시작 스케줄링
        scheduleRound(roomKey, result.getRound(), preview);

        // 로비 알림
        lobbyRoomNotificationService.notifyRoomStatusUpdated(
                roomId, result.getPlayers().size(), GameRoomStatus.PLAYING);

        return preview;
    }

    /**
     * 라운드가 종료된 뒤 다음 라운드를 준비할 때 사용한다.
     */
    public MultiRoadViewGameResponse.NextRound execute(Long roomId, Long gameId) {
        // 활성 플레이어 검증 (안전장치)
        if (cancelMultiGameService.cancelIfNoActivePlayers(roomId, gameId)) {
            log.info("Game cancelled before next round - no active players. RoomId: {}, GameId: {}", roomId, gameId);
            return null;
        }
        // 도메인 서비스에 라운드 준비 위임
        RoundPreparationService.NextRoundResult result =
                roundPreparationService.prepareNextRound(gameId);

        String roomKey = roomId.toString();
        long version = result.getRound().getRoundVersion();

        MultiRoadViewGameResponse.NextRound preview =
                MultiRoadViewGameResponse.NextRound.from(result.getGame(), result.getRound(), version);

        scheduleRound(roomKey, result.getRound(), preview);

        return preview;
    }

    /**
     * 라운드 번호에 따라 적절한 재발행 로직을 선택
     */
    public MultiRoadViewGameResponse.RoundProblem reissueRound(Long roomId,
                                                               Long gameId,
                                                               Long roundId,
                                                               Long expectedRoundVersion) {
        if (expectedRoundVersion == null) {
            throw new GameRoundHandler(ErrorStatus._BAD_REQUEST);
        }

        String roomKey = roomId.toString();
        Instant now = Instant.now();
        Instant cooldownThreshold = now.minusMillis(reissueCooldownMs);

        int casUpdated = roundPreparationService.tryAdvanceReissueVersion(
                roundId,
                gameId,
                expectedRoundVersion,
                maxReissueCountPerRound,
                cooldownThreshold,
                now
        );

        if (casUpdated == 0) {
            RoadViewGameRound latestRound = roundPreparationService.getRoundForReissue(roundId, gameId, roomId);
            long latestVersion = latestRound.getRoundVersion();
            log.info("Skip duplicate or blocked reissue request - RoomId: {}, RoundId: {}, ExpectedVersion: {}, CurrentVersion: {}",
                    roomKey, roundId, expectedRoundVersion, latestVersion);
            return MultiRoadViewGameResponse.RoundProblem.from(
                    latestRound.getMultiRoadViewGame(), latestRound, latestVersion, false);
        }

        RoadViewGameRound lockedRound = roundPreparationService.getRoundForReissueWithLock(roundId, gameId);
        roundPreparationService.validateOwnership(lockedRound, gameId, roomId);

        if (lockedRound.getServerStartTime() != null || lockedRound.getIsFinished()) {
            throw new GameRoundHandler(ErrorStatus._BAD_REQUEST);
        }

        RoundPreparationService.ReissueResult result =
                roundPreparationService.reissueRound(lockedRound, gameId);

        long version = result.getRound().getRoundVersion();
        MultiRoadViewGameResponse.RoundProblem problem =
                MultiRoadViewGameResponse.RoundProblem.from(
                        result.getGame(), result.getRound(), version, true);

        gameRoundNotificationService.broadcastRoundStart(roomKey, problem);
        log.info("Reissued round problem - RoomId: {}, RoundId: {}, Version: {}", roomKey, roundId, version);

        return problem;
    }

    private void scheduleRound(String roomKey, RoadViewGameRound round, Object preview) {
        // 프리뷰를 즉시 브로드캐스트
        gameRoundNotificationService.broadcastRoundStart(roomKey, preview);

        // 인트로 후 라운드 시작 스케줄링
        multiGameFlowScheduler.schedule(roomKey, MultiGameFlowScheduler.FlowTaskType.INTRO,
                Duration.ofMillis(INTRO_DURATION_MS),
                () -> runInTransaction(() -> startRound(roomKey, round.getId())));
    }

    private void startRound(String roomKey, Long roundId) {
        RoadViewGameRound round = roundPreparationService.startRound(roundId);
        gameTimerService.startRoundTimer(buildTimerCommand(roomKey, round));
    }

    private TimerCommand buildTimerCommand(String roomKey, RoadViewGameRound round) {
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

