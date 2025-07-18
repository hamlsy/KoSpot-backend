package com.kospot.application.game.roadview.rank.event;

import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.gamerank.vo.RankTier;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.point.vo.PointHistoryType;
import com.kospot.domain.point.service.PointHistoryService;
import com.kospot.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdatePointAndRankEvent {

    private final PointService pointService;
    private final MemberAdaptor memberAdaptor;
    private final PointHistoryService pointHistoryService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePointAndRank(Member member, RoadViewGame game, RankTier rankTier) {
        //earn point
        Member persistMember = memberAdaptor.queryById(member.getId()); // persist

        int point = pointService.addPointByRankGameScore(persistMember, rankTier, game.getScore());

        // save point history
        pointHistoryService.savePointHistory(persistMember, point, PointHistoryType.RANK_GAME);

    }
}
