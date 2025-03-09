package com.kospot.kospot.application.game.roadView.practice.usecase;

import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.event.RoadViewPracticeEvent;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@UseCase
@RequiredArgsConstructor
@Transactional
public class EndRoadViewPracticeUseCase {

    private final RoadViewGameService roadViewGameService;
    private final ApplicationEventPublisher eventPublisher;

    public EndGameResponse.RoadViewPractice execute(Member member, EndGameRequest.RoadView request) {
        RoadViewGame game = roadViewGameService.endGame(member, request);

        //event
        eventPublisher.publishEvent(new RoadViewPracticeEvent(member, game));

        return EndGameResponse.RoadViewPractice.from(game);
    }
}
