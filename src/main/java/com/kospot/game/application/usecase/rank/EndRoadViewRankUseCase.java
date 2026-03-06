package com.kospot.game.application.usecase.rank;


import com.kospot.game.application.adaptor.RoadViewGameAdaptor;
import com.kospot.game.application.service.RoadViewGameService;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.domain.event.RoadViewRankEvent;
import com.kospot.game.domain.vo.GameMode;
import com.kospot.game.presentation.dto.request.EndGameRequest;
import com.kospot.game.presentation.dto.response.EndGameResponse;
import com.kospot.gamerank.application.adaptor.GameRankAdaptor;
import com.kospot.gamerank.domain.entity.GameRank;
import com.kospot.gamerank.application.service.GameRankService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@UseCase
@RequiredArgsConstructor
@Transactional
public class EndRoadViewRankUseCase {

    private final MemberAdaptor memberAdaptor;
    private final RoadViewGameAdaptor roadViewGameAdaptor;
    private final RoadViewGameService roadViewGameService;
    private final GameRankAdaptor gameRankAdaptor;
    private final GameRankService gameRankService;
    private final ApplicationEventPublisher eventPublisher;

    public EndGameResponse.RoadViewRank execute(Long memberId, EndGameRequest.RoadView request) {
        Member member = memberAdaptor.queryById(memberId);
        GameRank gameRank = gameRankAdaptor.queryByMemberAndGameMode(member, GameMode.ROADVIEW);
        RoadViewGame game = roadViewGameAdaptor.queryByIdFetchCoordinate(request.getGameId());
        roadViewGameService.finishGame(member, game, request);

        int previousRatingScore = gameRank.getRatingScore();

        // calculate rating point
        gameRankService.updateRatingScoreAfterGameEnd(gameRank, game);

        //event
        eventPublisher.publishEvent(new RoadViewRankEvent(member, game, gameRank));

        return EndGameResponse.RoadViewRank.from(member, game, game.getCoordinate(), previousRatingScore, gameRank);
    }


}