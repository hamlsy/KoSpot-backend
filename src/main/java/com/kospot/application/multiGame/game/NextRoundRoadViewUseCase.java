package com.kospot.application.multiGame.game;

import com.kospot.domain.multiGame.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multiGame.game.entity.MultiRoadViewGame;
import com.kospot.domain.multiGame.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multiGame.gameRound.service.RoadViewGameRoundService;
import com.kospot.global.annotation.usecase.UseCase;
import com.kospot.presentation.multiGame.game.dto.response.MultiRoadViewGameResponse;
import com.kospot.presentation.multiGame.round.dto.request.GameRoundRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class NextRoundRoadViewUseCase {

    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GamePlayerAdaptor gamePlayerAdaptor;

    public MultiRoadViewGameResponse.NextRound execute(GameRoundRequest.NextRound request) {
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryByIdFetchGameRoom(request.getMultiGameId());
        RoadViewGameRound round = roadViewGameRoundService.createGameRound(game, request.getCurrentRound());
        List<GamePlayer> gamePlayers = gamePlayerAdaptor.queryByGameRoomId(game.getGameRoom().getId());
        return MultiRoadViewGameResponse.NextRound.from(game, round, gamePlayers);
    }

}
