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
        List<GamePlayer> players = gamePlayerAdaptor.queryByMultiRoadViewGameId(gameId);
        // 게임 종료 처리
        game.finishGame();

        // 최종 결과 생성 (순위 + 포인트 계산)
        MultiGameResponse.GameFinalResult finalResult = MultiGameResponse.GameFinalResult.from(gameId, players);
        
        // WebSocket으로 최종 결과 전송
        gameRoundNotificationService.notifyGameFinishedWithResults(gameRoomId, finalResult);
        
        // 포인트 지급을 위한 이벤트 발행
        MultiGameFinishedEvent event = MultiGameFinishedEvent.of(
                gameId,
                game.getGameMode(),
                game.getMatchType(),
                players
        );
        eventPublisher.publishEvent(event);
    }

}