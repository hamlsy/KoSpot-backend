package com.kospot.kospot.application.game.roadView.rank;

import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.game.event.RoadViewGameEvent;
import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.gameRank.adaptor.GameRankAdaptor;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@UseCase
@RequiredArgsConstructor
@Transactional
public class EndRoadViewRankUseCaseV2 {

    private final RoadViewGameService roadViewGameService;
    private final GameRankAdaptor gameRankAdaptor;
    private final ApplicationEventPublisher eventPublisher;

    public EndGameResponse.RoadViewRank execute(Member member, EndGameRequest.RoadView request) {
        GameRank gameRank = gameRankAdaptor.queryByMemberAndGameType(member, GameType.ROADVIEW);
        RoadViewGame game = roadViewGameService.endRankGameV2(member, gameRank, request);

        eventPublisher.publishEvent(new RoadViewGameEvent(member, game, gameRank));

        return EndGameResponse.RoadViewRank.from(game);
    }


}
