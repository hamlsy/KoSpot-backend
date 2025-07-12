package com.kospot.application.multiplayer.game;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multigame.game.entity.MultiRoadViewGame;
import com.kospot.domain.multigame.game.service.MultiRoadViewGameService;
import com.kospot.domain.multigame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multigame.gamePlayer.service.GamePlayerService;
import com.kospot.domain.multigame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multigame.gameRound.service.RoadViewGameRoundService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
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
public class StartMultiRoadViewPlayerGameUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final MultiRoadViewGameService multiRoadViewGameService;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GamePlayerService gamePlayerService;

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
        RoadViewGameRound roadViewGameRound = roadViewGameRoundService.createGameRound(game, 1);
        // 게임 플레이어 생성
        List<GamePlayer> gamePlayers = gamePlayerService.createRoadViewGamePlayers(gameRoom, game);
        return MultiRoadViewGameResponse.StartPlayerGame.from(game, roadViewGameRound, gamePlayers);
    }


}
