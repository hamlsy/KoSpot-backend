package com.kospot.kospot.application.game.roadView.practice;

import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.entity.PointHistoryType;
import com.kospot.kospot.domain.point.service.PointHistoryService;
import com.kospot.kospot.domain.point.service.PointService;
import com.kospot.kospot.domain.point.util.PointCalculator;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class EndRoadViewPracticeUseCase {

    private final RoadViewGameService roadViewGameService;
    private final PointService pointService;
    private final PointHistoryService pointHistoryService;

    // todo refactor transaction
    public EndGameResponse.RoadViewPractice execute(Member member, EndGameRequest.RoadView request) {
        RoadViewGame game = roadViewGameService.endPracticeGame(member, request);

        // add point
        int point = PointCalculator.getPracticePoint(game.getScore());
        pointService.addPoint(member, point);

        // save point history
        pointHistoryService.savePointHistory(member, point, PointHistoryType.PRACTICE_GAME);

        return EndGameResponse.RoadViewPractice.from(game);
    }
}
