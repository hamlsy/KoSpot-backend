package com.kospot.application.multi.round.roadview;

import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.event.MultiGameFinishedEvent;
import com.kospot.domain.multi.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.multi.round.service.GameRoundNotificationService;
import com.kospot.presentation.multi.game.dto.response.MultiGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class FinishMultiRoadViewGameUseCase {

    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final GamePlayerAdaptor gamePlayerAdaptor;
    private final GameRoundNotificationService gameRoundNotificationService;
    private final ApplicationEventPublisher eventPublisher;

    public void execute(String gameRoomId, Long gameId) {
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(gameId);
        List<GamePlayer> players = gamePlayerAdaptor.queryByMultiRoadViewGameIdWithMember(gameId);
        game.finishGame();

        MultiGameResponse.GameFinalResult finalResult = MultiGameResponse.GameFinalResult.from(gameId, players);
        gameRoundNotificationService.notifyGameFinishedWithResults(gameRoomId, finalResult);
        
        MultiGameFinishedEvent event = MultiGameFinishedEvent.of(
                gameId,
                game.getGameMode(),
                game.getMatchType(),
                players
        );
        eventPublisher.publishEvent(event);
    }

}