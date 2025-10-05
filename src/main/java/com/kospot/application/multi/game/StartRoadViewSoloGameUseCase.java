package com.kospot.application.multi.game;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.service.MultiRoadViewGameService;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.service.GamePlayerService;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.round.entity.BaseGameRound;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.service.RoadViewGameRoundService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.multi.timer.service.GameTimerService;
import com.kospot.infrastructure.websocket.domain.multi.timer.vo.TimerCommand;
import com.kospot.presentation.multigame.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multigame.game.dto.response.MultiRoadViewGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class StartRoadViewSoloGameUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final MultiRoadViewGameService multiRoadViewGameService;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GamePlayerService gamePlayerService;
    private final GameTimerService gameTimerService;

    public MultiRoadViewGameResponse.StartPlayerGame execute(Member host, MultiGameRequest.Start request) {
        GameRoom gameRoom = gameRoomAdaptor.queryByIdFetchHost(request.getGameRoomId());
        gameRoom.start(host);
        return startRoadViewGame(gameRoom, request);
    }

    private MultiRoadViewGameResponse.StartPlayerGame startRoadViewGame(GameRoom gameRoom, MultiGameRequest.Start request) {
        // 로드뷰 게임 생성
        MultiRoadViewGame game = multiRoadViewGameService.createGame(gameRoom, request);
        // 게임 시작
        game.startGame();

        // 라운드 생성 (모드별 파라미터 전달)
        //todo 플레이어 아이디 리스트 전달? -> redis에서 가져옴
        RoadViewGameRound roadViewGameRound = roadViewGameRoundService.createGameRound(game, 1, request.getTimeLimit());

        // 타이머 커맨드 생성
        TimerCommand command = createTimerCommand(request, game, roadViewGameRound);

        List<GamePlayer> gamePlayers = gamePlayerService.createRoadViewGamePlayers(gameRoom, game);
        gameTimerService.startRoundTimer(command);
        return MultiRoadViewGameResponse.StartPlayerGame.from(game, roadViewGameRound, gamePlayers);
    }

    private TimerCommand createTimerCommand(MultiGameRequest.Start request, MultiRoadViewGame game, BaseGameRound round) {
        return TimerCommand.builder()
                .round(round)
                .gameRoomId()
                .gameId()
                .build();
    }

}
