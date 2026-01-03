package com.kospot.application.multi.round.roadview;

import com.kospot.application.multi.flow.MultiGameFlowScheduler;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.multi.game.service.MultiGameRedisService;
import com.kospot.infrastructure.websocket.domain.multi.lobby.service.LobbyRoomNotificationService;
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
    private final MultiGameRedisService multiGameRedisService;
    private final GameRoundNotificationService gameRoundNotificationService;
    private final LobbyRoomNotificationService lobbyRoomNotificationService;
    private final MultiGameFlowScheduler multiGameFlowScheduler;
    private final GameTimerService gameTimerService;
    private final PlatformTransactionManager transactionManager;

    /**
     * 모든 플레이어 로딩이 끝난 직후 호출되어 1라운드를 준비한다.
     */
    public MultiRoadViewGameResponse.StartPlayerGame executeInitial(Long roomId, Long gameId) {
        // 도메인 서비스에 라운드 준비 위임
        RoundPreparationService.InitialRoundResult result =
                roundPreparationService.prepareInitialRound(gameId);

        if (result == null) {
            log.info("Initial round already prepared - RoomId: {}, GameId: {}", roomId, gameId);
            return null;
        }

        String roomKey = roomId.toString();
        long version = multiGameRedisService.incrementRoundVersion(roomKey, result.getRound().getId());

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
        // 도메인 서비스에 라운드 준비 위임
        RoundPreparationService.NextRoundResult result =
                roundPreparationService.prepareNextRound(gameId);

        String roomKey = roomId.toString();
        long version = multiGameRedisService.incrementRoundVersion(roomKey, result.getRound().getId());

        MultiRoadViewGameResponse.NextRound preview =
                MultiRoadViewGameResponse.NextRound.from(result.getGame(), result.getRound(), version);

        scheduleRound(roomKey, result.getRound(), preview);

        return preview;
    }

    /**
     * 라운드 번호에 따라 적절한 재발행 로직을 선택
     */
    public MultiRoadViewGameResponse.RoundProblem reissueRound(Long roomId, Long gameId, Long roundId) {
        String roomKey = roomId.toString();

        if (!multiGameRedisService.acquireRoundReissueLock(roomKey, roundId)) {
            // 락 획득 실패 시 현재 버전 반환
            RoundPreparationService.ReissueResult existing =
                    roundPreparationService.reissueRound(roundId, gameId);
            long version = multiGameRedisService.getRoundVersion(roomKey, roundId);
            return MultiRoadViewGameResponse.RoundProblem.from(
                    existing.getGame(), existing.getRound(), version);
        }

        try {
            RoundPreparationService.ReissueResult result =
                    roundPreparationService.reissueRound(roundId, gameId);

            long version = multiGameRedisService.incrementRoundVersion(roomKey, roundId);
            MultiRoadViewGameResponse.RoundProblem problem =
                    MultiRoadViewGameResponse.RoundProblem.from(
                            result.getGame(), result.getRound(), version);

            gameRoundNotificationService.broadcastRoundStart(roomKey, problem);
            log.info("Reissued round problem - RoomId: {}, RoundId: {}", roomKey, roundId);

            return problem;
        } finally {
            multiGameRedisService.releaseRoundReissueLock(roomKey, roundId);
        }
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
