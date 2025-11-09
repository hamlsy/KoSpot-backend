package com.kospot.application.multi.game.usecase;

import com.kospot.domain.game.service.RoadViewGameService;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.service.MultiRoadViewGameService;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.service.GamePlayerService;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.service.RoadViewGameRoundService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multi.round.dto.response.RoadViewRoundResponse;
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
    private final MultiRoadViewGameService multiRoadViewGameService;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GamePlayerService gamePlayerService;

    public void execute(Long roomId, MultiGameRequest.Start request) {

        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(request.getGameId());
        GameRoom gameRoom = gameRoomAdaptor.queryById(roomId);

        // 첫번째 라운드 생성
        game.startGame();
        RoadViewGameRound roadViewGameRound = roadViewGameRoundService.createGameRound(game, playerIds);
        RoadViewRoundResponse.Info roundInfo = RoadViewRoundResponse.Info.from(roadViewGameRound);

        // 플레이어 저장
        List<GamePlayer> gamePlayers = gamePlayerService.createRoadViewGamePlayers(gameRoom, game);
        List<Long> playerIds = gamePlayers.stream().map(GamePlayer::getId).toList();

        // 첫번째 라운드 시작

    }

}
