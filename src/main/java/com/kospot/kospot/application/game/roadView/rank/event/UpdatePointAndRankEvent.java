package com.kospot.kospot.application.game.roadView.rank.event;

import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.gameRank.adaptor.GameRankAdaptor;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.gameRank.service.GameRankService;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.entity.PointHistoryType;
import com.kospot.kospot.domain.point.service.PointHistoryService;
import com.kospot.kospot.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class UpdatePointAndRankEvent {

    private final PointService pointService;
    private final GameRankAdaptor gameRankAdaptor;
    private final PointHistoryService pointHistoryService;
    private final GameRankService gameRankService;

    public void updatePointAndRank(Member member, RoadViewGame game){
        // earn point
        GameRank gameRank = gameRankAdaptor.queryByMemberAndGameType(member, GameType.ROADVIEW);

        // persist
        Member persistMember = gameRank.getMember();

        int point = pointService.addPointByRankGameScore(persistMember, gameRank, game.getScore());

        // calculate rating point
        gameRankService.updateRatingScoreAfterGameEndV2(gameRank, game);

        // save point history
        pointHistoryService.savePointHistory(persistMember, point, PointHistoryType.RANK_GAME);
    }
}
