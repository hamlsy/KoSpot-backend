package com.kospot.application.multi.game;

import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.service.RoadViewGameRoundService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.multigame.game.dto.response.MultiRoadViewGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class NextRoadViewRoundUseCase {

    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final RoadViewGameRoundService roadViewGameRoundService;

    public MultiRoadViewGameResponse.NextRound execute(Long multiGameId, int nextRound) {
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(multiGameId);
        game.moveToNextRound();
        RoadViewGameRound round = roadViewGameRoundService.createGameRound(game, nextRound);
        return MultiRoadViewGameResponse.NextRound.from(game, round);
    }

}
