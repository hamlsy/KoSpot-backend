package com.kospot.application.multi.round.listener;

import com.kospot.application.multi.round.roadview.solo.EndRoadViewSoloRoundUseCase;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.room.vo.GameRoomNotification;
import com.kospot.domain.multi.submission.event.EarlyRoundCompletionEvent;
import com.kospot.domain.multi.timer.event.RoundCompletionEvent;
import com.kospot.infrastructure.websocket.domain.multi.round.service.GameRoundNotificationService;
import com.kospot.presentation.multi.round.dto.response.RoadViewRoundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.kospot.domain.multi.game.vo.PlayerMatchType.TEAM;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoundCompletionEventListener {
    // UseCases
    private final EndRoadViewSoloRoundUseCase endRoadViewSoloRoundUseCase;

    // ===

    private final GameRoundNotificationService gameRoundNotificationService;


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
                    Long.parseLong(event.getGameId()),
                    Long.parseLong(event.getRoundId()),
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
            }
            case TEAM -> log.warn("Team mode not yet implemented");
        }
    }

}
