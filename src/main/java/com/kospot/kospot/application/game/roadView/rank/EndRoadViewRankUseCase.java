package com.kospot.kospot.application.game.roadView.rank;

import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.gameRank.adaptor.GameRankAdaptor;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.entity.PointHistoryType;
import com.kospot.kospot.domain.point.service.PointHistoryService;
import com.kospot.kospot.domain.point.service.PointService;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class EndRoadViewRankUseCase {

    private final RoadViewGameService roadViewGameService;
    private final PointService pointService;
    private final GameRankAdaptor gameRankAdaptor;
    private final PointHistoryService pointHistoryService;

    //todo refactor transaction
    public EndGameResponse.RoadViewRank execute(Member member, EndGameRequest.RoadView request){
        // end
        RoadViewGame game = roadViewGameService.endRankGame(member, request);

        // earn point
        GameRank gameRank = gameRankAdaptor.queryByMemberAndGameType(member, GameType.ROADVIEW);
        int point = pointService.addPointByRankGameScore(member, gameRank, game.getScore());

        // save point history
        pointHistoryService.savePointHistory(member, point, PointHistoryType.RANK_GAME);

        return EndGameResponse.RoadViewRank.from(game);
    }

}
