package com.kospot.kospot.application.game.roadView.rank.usecase;

import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.game.event.RoadViewRankEvent;
import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.gameRank.adaptor.GameRankAdaptor;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.gameRank.service.GameRankService;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@UseCase
@RequiredArgsConstructor
@Transactional
public class EndRoadViewRankUseCase {

    private final RoadViewGameService roadViewGameService;
    private final GameRankAdaptor gameRankAdaptor;
    private final GameRankService gameRankService;
    private final ApplicationEventPublisher eventPublisher;

    public EndGameResponse.RoadViewRank execute(Member member, EndGameRequest.RoadView request) {
        GameRank gameRank = gameRankAdaptor.queryByMemberAndGameType(member, GameType.ROADVIEW);
        RoadViewGame game = roadViewGameService.endGame(member, request);

        int currentRatingScore = gameRank.getRatingScore();

        // calculate rating point
        gameRankService.updateRatingScoreAfterGameEnd(gameRank, game);

        //event
        eventPublisher.publishEvent(new RoadViewRankEvent(member, game, gameRank));

        return EndGameResponse.RoadViewRank.fromV2(game, currentRatingScore, gameRank.getRatingScore());
    }


}
