package com.kospot.application.multi.round.roadview;

import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.multi.round.service.GameRoundNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class FinishMultiRoadViewGameUseCase {

    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final GameRoundNotificationService gameRoundNotificationService;

    public void execute(String gameRoomId, Long gameId) {
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(gameId);
        game.finishGame();
        gameRoundNotificationService.notifyGameFinished(gameRoomId, game.getId());
    }

}