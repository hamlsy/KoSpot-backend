package com.kospot.application.multi.round.listener;

import com.kospot.application.multi.round.roadview.solo.EndRoadViewSoloRoundUseCase;
import com.kospot.domain.multi.room.vo.GameRoomNotification;
import com.kospot.domain.multi.submission.event.EarlyRoundCompletionEvent;
import com.kospot.infrastructure.websocket.domain.multi.round.service.GameRoundNotificationService;
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

    // ===

    private final GameRoundNotificationService gameRoundNotificationService;


    @Async
    @EventListener
    @Transactional
    public void handleEarlyRoundCompletion(EarlyRoundCompletionEvent event) {
        switch (event.getGameMode()) {
            case ROADVIEW -> handleRoadViewRoundCompletion(event);
            case PHOTO -> {
            }
        }
    }

    private void handleRoadViewRoundCompletion(EarlyRoundCompletionEvent event) {
        Long gameId = event.getGameId();
        Long roundId = event.getRoundId();

        switch (event.getPlayerMatchType()) {
            case SOLO -> {// 1. 라운드 결과 계산 및 순위 산정
                RoadViewRoundResponse.PlayerResult result = endRoadViewSoloRoundUseCase.execute(gameId, roundId);
                // 2. WebSocket 결과 브로드캐스트
                gameRoundNotificationService.broadcastRoundResults(event.getGameRoomId(), result);
            }
        }


    }

}
