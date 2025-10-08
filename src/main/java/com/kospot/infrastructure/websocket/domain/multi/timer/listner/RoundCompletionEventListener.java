package com.kospot.infrastructure.websocket.domain.multi.timer.listner;

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
//        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(event.getGameId());
        // if !game.isLastRound()
        // -> 라운드 결과 -> endRoadViewSoloRoundUseCase
        switch (event.getPlayerMatchType()) {
            case SOLO -> {
                // endRoadViewSoloRoundUseCase.execute(event.getGameRoomId(), event.getRoundId());
            }
        }

    }

}
