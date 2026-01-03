package com.kospot.application.multi.round.listener;

import com.kospot.application.multi.game.usecase.FinishMultiRoadViewGameUseCase;
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

/**
 * 라운드 완료 이벤트를 처리하는 리스너
 * 도메인 이벤트를 수신하여 라운드 종료/다음 라운드/게임 종료를 조율한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoundCompletionEventListener {

    // UseCases
    private final EndRoadViewSoloRoundUseCase endRoadViewSoloRoundUseCase;
    private final NextRoadViewRoundUseCase nextRoadViewRoundUseCase;
    private final FinishMultiRoadViewGameUseCase finishMultiRoadViewGameUseCase;

    // Domain Adaptor
    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;

    // Infrastructure Services (직접 사용)
    private final GameRoundNotificationService gameRoundNotificationService;
    private final GameTimerService gameTimerService;

    /**
     * 조기 종료 이벤트 처리 (모든 플레이어가 제출 완료)
     */
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
            case PHOTO -> log.debug("Photo mode early completion not yet implemented");
        }
    }

    /**
     * 정상 종료 이벤트 처리 (타이머 만료)
     */
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
            case PHOTO -> log.debug("Photo mode completion not yet implemented");
        }
    }

    private void handleRoadViewRoundCompletion(String gameRoomId, Long gameId,
                                               Long roundId, PlayerMatchType matchType) {
        switch (matchType) {
            case SOLO -> {
                RoadViewRoundResponse.PlayerResult result =
                        endRoadViewSoloRoundUseCase.execute(gameId, roundId);
                gameRoundNotificationService.broadcastRoadViewSoloRoundResults(gameRoomId, result);

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
        switch (game.getGameMode()) {
            case ROADVIEW -> finishMultiRoadViewGameUseCase.execute(gameRoomId, game.getId());
            case PHOTO -> log.debug("Photo mode finish not yet implemented");
        }
    }

    /**
     * 다음 라운드 처리 - 새 라운드 시작
     */
    private void handleNextRound(String gameRoomId, MultiGame game) {
        switch (game.getGameMode()) {
            case ROADVIEW -> {
                MultiRoadViewGameResponse.NextRound nextRound = nextRoadViewRoundUseCase.execute(
                        Long.parseLong(gameRoomId),
                        game.getId()
                );
                // NextRoadViewRoundUseCase 내부에서 이미 브로드캐스트하므로 여기서는 생략
                log.info("Next round started - RoomId: {}, GameId: {}, Round: {}",
                        gameRoomId, game.getId(), nextRound.getCurrentRound());
            }
            case PHOTO -> log.debug("Photo mode next round not yet implemented");
        }
    }
}
