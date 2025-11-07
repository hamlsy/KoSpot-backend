package com.kospot.application.game.roadview.rank.usecase;

import com.kospot.domain.game.adaptor.RoadViewGameAdaptor;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.game.event.RoadViewRankEvent;
import com.kospot.presentation.game.dto.request.EndGameRequest;
import com.kospot.presentation.game.dto.response.EndGameResponse;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.service.RoadViewGameService;
import com.kospot.domain.gamerank.adaptor.GameRankAdaptor;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.gamerank.service.GameRankService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@UseCase
@RequiredArgsConstructor
@Transactional
public class EndRoadViewRankUseCase {

    private final RoadViewGameAdaptor roadViewGameAdaptor;
    private final RoadViewGameService roadViewGameService;
    private final GameRankAdaptor gameRankAdaptor;
    private final GameRankService gameRankService;
    private final ApplicationEventPublisher eventPublisher;

    public EndGameResponse.RoadViewRank execute(Member member, EndGameRequest.RoadView request) {
        GameRank gameRank = gameRankAdaptor.queryByMemberAndGameMode(member, GameMode.ROADVIEW);
        RoadViewGame game = roadViewGameAdaptor.queryByIdFetchCoordinate(request.getGameId());
        roadViewGameService.finishGame(member, game, request);

        int previousRatingScore = gameRank.getRatingScore();

        // calculate rating point
        gameRankService.updateRatingScoreAfterGameEnd(gameRank, game);

        //event
        eventPublisher.publishEvent(new RoadViewRankEvent(member, game, gameRank));

        return EndGameResponse.RoadViewRank.from(game, game.getCoordinate(), previousRatingScore, gameRank);
    }


}
