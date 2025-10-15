package com.kospot.infrastructure.websocket.domain.multi.timer.listner;

import com.kospot.application.multi.round.roadview.solo.EndRoadViewSoloRoundUseCase;
import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.infrastructure.redis.domain.multi.timer.event.RoundCompletionEvent;
import com.kospot.infrastructure.websocket.domain.multi.timer.service.GameTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoundCompletionEventListener {

    // 각 게임 모드별 UseCase 주입
    // private final NextRoadViewRoundUseCase nextRoadViewRoundUseCase;
    // private final NextPhotoRoundUseCase nextPhotoRoundUseCase; // 향후 구현
    // private final EndPhotoSoloRoundUseCase endPhotoSoloRoundUseCase; // 향후 구현
    private final GameTimerService gameTimerService;
    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final EndRoadViewSoloRoundUseCase endRoadViewSoloRoundUseCase;

    @Async
    @EventListener
    public void handleRoundCompletion(RoundCompletionEvent event) {

        switch (event.getGameMode()) {
            case ROADVIEW -> handleRoadViewRoundCompletion(event);
//            case PHOTO -> handlePhotoRoundCompletion(event);
        }

    }

    //todo implement
    private void handleRoadViewRoundCompletion(RoundCompletionEvent event) {
        // 현재 라운드가 마지막 라운드인지 확인
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(Long.parseLong(event.getGameId()));
        if (!game.isLastRound()) {
            return;
        }
        switch (event.getPlayerMatchType()) {
            case SOLO -> {
                 endRoadViewSoloRoundUseCase.execute(Long.parseLong(event.getGameId())
                         , Long.parseLong(event.getRoundId()));
            }
            case TEAM -> {
                return;
            }
        }

    }

}
