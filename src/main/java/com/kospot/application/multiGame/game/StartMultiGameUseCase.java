package com.kospot.application.multiGame.game;

import com.kospot.domain.game.entity.GameMode;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multiGame.game.entity.MultiPhotoGame;
import com.kospot.domain.multiGame.game.entity.MultiRoadViewGame;
import com.kospot.domain.multiGame.game.service.MultiRoadViewGameService;
import com.kospot.domain.multiGame.game.service.PhotoGameService;
import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gamePlayer.service.GamePlayerService;
import com.kospot.domain.multiGame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multiGame.gameRound.service.RoadViewGameRoundService;
import com.kospot.global.annotation.usecase.UseCase;
import com.kospot.presentation.multiGame.game.dto.request.MultiGameRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class StartMultiGameUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final MultiRoadViewGameService multiRoadViewGameService;
    private final PhotoGameService photoGameService;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GamePlayerService gamePlayerService;

    public void execute(Member host, MultiGameRequest.Start request) {
        GameRoom gameRoom = gameRoomAdaptor.queryByIdFetchHost(request.getGameRoomId());
        gameRoom.isHost(host);
        GameMode gameMode = GameMode.fromKey(request.getGameModeKey());
        switch (gameMode) {
            case ROADVIEW -> startRoadViewGame(gameRoom, request);
            case PHOTO -> startPhotoGame(gameRoom, request);
        }

    }


    private void startRoadViewGame(GameRoom gameRoom, MultiGameRequest.Start request) {
        // 로드뷰 게임 생성
        MultiRoadViewGame game = multiRoadViewGameService.createGame(gameRoom, request);
        // 라운드 생성 (모드별 파라미터 전달)
        RoadViewGameRound roadViewGameRound = roadViewGameRoundService.createGameRound(game, 1);
        // 게임 플레이어 생성
        List<GamePlayer> gamePlayers = gamePlayerService.createGamePlayers(gameRoom);

    }

    private void startPhotoGame(GameRoom gameRoom, MultiGameRequest.Start request) {
        // 포토 게임 생성
        MultiPhotoGame game = photoGameService.createGame(gameRoom, request);
        // 라운드 생성 (모드별 파라미터 전달)
        roundService.createInitialRounds(game, request.getRoundCount());
        // 게임 시작 (모드별 특수 로직)
        photoGameService.startGame(game, gamePlayers);
    }
}
