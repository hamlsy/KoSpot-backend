package com.kospot.kospot.application.game.roadView.rank;

import com.kospot.kospot.domain.game.event.RoadViewGameEvent;
import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@UseCase
@RequiredArgsConstructor
@Transactional
public class EndRoadViewRankUseCaseV2 {

    private final RoadViewGameService roadViewGameService;
    private final ApplicationEventPublisher eventPublisher;

    public EndGameResponse.RoadViewRank execute(Member member, EndGameRequest.RoadView request) {
        // end game todo add rating score
        RoadViewGame game = roadViewGameService.endRankGame(member, request);

        eventPublisher.publishEvent(new RoadViewGameEvent(member, game));

        return EndGameResponse.RoadViewRank.from(game);
    }


}
