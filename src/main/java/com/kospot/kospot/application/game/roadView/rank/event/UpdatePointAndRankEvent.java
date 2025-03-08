package com.kospot.kospot.application.game.roadView.rank.event;

import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.gameRank.adaptor.GameRankAdaptor;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.gameRank.entity.RankTier;
import com.kospot.kospot.domain.gameRank.service.GameRankService;
import com.kospot.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.entity.PointHistoryType;
import com.kospot.kospot.domain.point.service.PointHistoryService;
import com.kospot.kospot.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdatePointAndRankEvent {

    private final PointService pointService;
    private final GameRankAdaptor gameRankAdaptor;
    private final MemberAdaptor memberAdaptor;
    private final PointHistoryService pointHistoryService;
    private final GameRankService gameRankService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePointAndRank(Member member, RoadViewGame game, RankTier rankTier){
        //earn point

        // persist
        Member persistMember = memberAdaptor.queryById(member.getId());

        int point = pointService.addPointByRankGameScoreV2(persistMember, rankTier, game.getScore());

        // save point history
        pointHistoryService.savePointHistory(persistMember, point, PointHistoryType.RANK_GAME);
    }
}
