package com.kospot.application.game.roadview.history.usecase;

import com.kospot.domain.game.adaptor.RoadViewGameAdaptor;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.game.dto.response.RoadViewGameHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRecentThreeRoadViewGamesUseCase {

    private final RoadViewGameAdaptor roadViewGameAdaptor;

    public RoadViewGameHistoryResponse.RecentThree execute(Member member) {
        List<RoadViewGame> games = roadViewGameAdaptor.queryRecentThreeGamesByMember(member);
        return RoadViewGameHistoryResponse.RecentThree.from(games);
    }
}

