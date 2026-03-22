package com.kospot.game.application.usecase.practice.usecase;

import com.kospot.game.application.adaptor.RoadViewGameAdaptor;
import com.kospot.game.infrastructure.redis.adaptor.AnonymousPracticeTokenRedisAdaptor;
import com.kospot.game.presentation.dto.request.EndGameRequest;
import com.kospot.game.presentation.dto.response.EndGameResponse;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.domain.event.RoadViewPracticeEvent;
import com.kospot.game.application.service.RoadViewGameService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@UseCase
@RequiredArgsConstructor
@Transactional
public class EndRoadViewPracticeUseCase {

    private final MemberAdaptor memberAdaptor;
    private final RoadViewGameAdaptor roadViewGameAdaptor;
    private final RoadViewGameService roadViewGameService;
    private final ApplicationEventPublisher eventPublisher;
    private final AnonymousPracticeTokenRedisAdaptor anonymousPracticeTokenRedisAdaptor;

    public EndGameResponse.RoadViewPractice execute(Long memberId,
                                                     EndGameRequest.RoadView request,
                                                     String practiceToken) {
        RoadViewGame game = roadViewGameAdaptor.queryByIdFetchCoordinate(request.getGameId());

        if (memberId != null) {
            return endAsLoggedIn(memberId, game, request);
        }
        return endAsAnonymous(game, request, practiceToken);
    }

    private EndGameResponse.RoadViewPractice endAsLoggedIn(Long memberId,
                                                            RoadViewGame game,
                                                            EndGameRequest.RoadView request) {
        Member member = memberAdaptor.queryById(memberId);
        roadViewGameService.finishGame(member, game, request);
        eventPublisher.publishEvent(new RoadViewPracticeEvent(member, game));
        return EndGameResponse.RoadViewPractice.from(member, game, game.getCoordinate());
    }

    private EndGameResponse.RoadViewPractice endAsAnonymous(RoadViewGame game,
                                                             EndGameRequest.RoadView request,
                                                             String practiceToken) {
        anonymousPracticeTokenRedisAdaptor.validate(game.getId(), practiceToken);
        roadViewGameService.finishGameAnonymous(game, request);
        anonymousPracticeTokenRedisAdaptor.delete(game.getId());
        return EndGameResponse.RoadViewPractice.fromAnonymous(game, game.getCoordinate());
    }
}
