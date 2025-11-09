package com.kospot.application.multi.game.usecase;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.service.GamePlayerService;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.service.RoadViewGameRoundService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.multi.round.service.GameRoundNotificationService;
import com.kospot.infrastructure.websocket.domain.multi.timer.service.GameTimerService;
import com.kospot.infrastructure.websocket.domain.multi.timer.vo.TimerCommand;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class StartRoadViewSoloGameUseCase {

    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final GameRoomAdaptor gameRoomAdaptor;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GamePlayerService gamePlayerService;
    private final GameRoundNotificationService gameRoundNotificationService;
    private final GameTimerService gameTimerService;

    /**
     * 모든 플레이어 준비 완료 이후 실제 1라운드를 생성하고 타이머를 시작한다.
     */
    public MultiRoadViewGameResponse.StartPlayerGame execute(Long roomId, Long gameId) {
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(gameId);
        if (game.isInProgress()) {
            log.info("Game already in progress. Skip start flow - GameId: {}", gameId);
            return null;
        }

        game.startGame();

        GameRoom gameRoom = gameRoomAdaptor.queryById(roomId);
        List<GamePlayer> gamePlayers = gamePlayerService.findPlayersByGameId(game.getId());
        List<Long> playerIds = gamePlayers.stream()
                .map(GamePlayer::getId)
                .toList();

        RoadViewGameRound roadViewGameRound = roadViewGameRoundService.createGameRound(game, playerIds);

        MultiRoadViewGameResponse.StartPlayerGame response =
                MultiRoadViewGameResponse.StartPlayerGame.from(game, roadViewGameRound, gamePlayers);

        gameRoundNotificationService.broadcastRoundStart(gameRoom.getId().toString(), response);

        TimerCommand timerCommand = TimerCommand.builder()
                .round(roadViewGameRound)
                .gameRoomId(gameRoom.getId().toString())
                .gameId(game.getId())
                .gameMode(GameMode.ROADVIEW)
                .matchType(PlayerMatchType.SOLO)
                .build();
        gameTimerService.startRoundTimer(timerCommand);

        return response;
    }
}
