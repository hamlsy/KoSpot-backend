package com.kospot.application.game.roadview.practice.usecase;

import com.kospot.domain.game.adaptor.RoadViewGameAdaptor;
import com.kospot.presentation.game.dto.request.EndGameRequest;
import com.kospot.presentation.game.dto.response.EndGameResponse;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.event.RoadViewPracticeEvent;
import com.kospot.domain.game.service.RoadViewGameService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@UseCase
@RequiredArgsConstructor
@Transactional
public class EndRoadViewPracticeUseCase {

    private final RoadViewGameAdaptor roadViewGameAdaptor;
    private final RoadViewGameService roadViewGameService;
    private final ApplicationEventPublisher eventPublisher;

    public EndGameResponse.RoadViewPractice execute(Member member, EndGameRequest.RoadView request) {
        RoadViewGame game = roadViewGameAdaptor.queryByIdFetchCoordinate(request.getGameId());
        roadViewGameService.endGame(member, game, request);

        //event
        eventPublisher.publishEvent(new RoadViewPracticeEvent(member, game));

        return EndGameResponse.RoadViewPractice.from(game);
    }
}
