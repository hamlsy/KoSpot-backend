package com.kospot.application.multi.round.listener;

import com.kospot.application.multi.round.roadview.NextRoadViewRoundUseCase;
import com.kospot.application.multi.round.roadview.solo.EndRoadViewSoloRoundUseCase;
import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiGame;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.submission.event.EarlyRoundCompletionEvent;
import com.kospot.domain.multi.timer.event.RoundCompletionEvent;
import com.kospot.infrastructure.websocket.domain.multi.round.service.GameRoundNotificationService;
import com.kospot.infrastructure.websocket.domain.multi.timer.service.GameTimerService;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import com.kospot.presentation.multi.round.dto.response.RoadViewRoundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
@RequiredArgsConstructor
public class RoundCompletionEventListener {
    // UseCases
    private final EndRoadViewSoloRoundUseCase endRoadViewSoloRoundUseCase;
    private final NextRoadViewRoundUseCase nextRoadViewRoundUseCase;

    // ===
    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final GameRoundNotificationService gameRoundNotificationService;
    private final GameTimerService gameTimerService;

    // 조기 종료 이벤트
    @Async
    @EventListener
    @Transactional
    public void handleEarlyRoundCompletion(EarlyRoundCompletionEvent event) {
        switch (event.getGameMode()) {
            case ROADVIEW -> handleRoadViewRoundCompletion(
                    event.getGameRoomId(),
                    event.getGameId(),
                    event.getRoundId(),
                    event.getPlayerMatchType()
            );
            case PHOTO -> {
            }
        }
    }

    // 정상 종료 이벤트 처리 (타이머 만료)
    @Async
    @EventListener
    @Transactional
    public void handleRoundCompletion(RoundCompletionEvent event) {
        switch (event.getGameMode()) {
            case ROADVIEW -> handleRoadViewRoundCompletion(
                    event.getGameRoomId(),
                    event.getGameId(),
                    event.getRoundId(),
                    event.getPlayerMatchType()
            );
            case PHOTO -> {
            }
        }

    }

    private void handleRoadViewRoundCompletion(String gameRoomId, Long gameId,
                                               Long roundId, PlayerMatchType matchType) {
        switch (matchType) {
            case SOLO -> {
                RoadViewRoundResponse.PlayerResult result = endRoadViewSoloRoundUseCase.execute(gameId, roundId);
                gameRoundNotificationService.broadcastRoundResults(gameRoomId, result);

                MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(gameId);
                startTransitionTimer(gameRoomId, game);
            }
            case TEAM -> log.warn("Team mode not yet implemented");
        }
    }

    private void startTransitionTimer(String gameRoomId, MultiGame game) {
        gameTimerService.startRoundTransitionTimer(gameRoomId, game, () -> {
            processNextRound(gameRoomId, game);
        });
    }

    private void processNextRound(String gameRoomId, MultiGame game) {
        if (game.isLastRound()) {
            handleLastRound(gameRoomId, game);
            return;
        }
        handleNextRound(gameRoomId, game);
    }

    /**
     * 마지막 라운드 처리 - 게임 종료
     */
    private void handleLastRound(String gameRoomId, MultiGame game) {
        game.finishGame();
        // add finish game usecase
        gameRoundNotificationService.notifyGameFinished(gameRoomId, game.getId());
    }

    /**
     * 다음 라운드 처리 - 새 라운드 시작
     */
    private void handleNextRound(String gameRoomId, MultiGame game) {

        switch (game.getGameMode()) {
            case ROADVIEW -> {
                // 다음 라운드 시작 (게임 상태 업데이트 + 라운드 생성 + 타이머 시작)
                MultiRoadViewGameResponse.NextRound nextRound = nextRoadViewRoundUseCase.execute(
                        Long.parseLong(gameRoomId),
                        game.getId()
                );

                // 다음 라운드 시작 알림 브로드캐스트
                gameRoundNotificationService.broadcastRoundStart(gameRoomId, nextRound);
            }
            case PHOTO -> {
            }
        }

    }


}
