package com.kospot.kospot.application.game.roadView.rank.listener;

import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.gameRank.adaptor.GameRankAdaptor;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.gameRank.repository.GameRankRepository;
import com.kospot.kospot.domain.gameRank.service.GameRankService;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.entity.PointHistoryType;
import com.kospot.kospot.domain.point.service.PointHistoryService;
import com.kospot.kospot.domain.point.service.PointService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class UpdatePointAndRankEvent {

    private final RoadViewGameService roadViewGameService;
    private final PointService pointService;
    private final GameRankAdaptor gameRankAdaptor;
    private final PointHistoryService pointHistoryService;
    private final GameRankService gameRankService;
    private final EntityManager em;
    private final GameRankRepository gameRankRepository;

    public void updatePointAndRank(Member member, RoadViewGame game){
        // earn point
        GameRank gameRank = gameRankAdaptor.queryByMemberAndGameType(member, GameType.ROADVIEW);

        // merge
        Member mergedMember = gameRank.getMember();

        int point = pointService.addPointByRankGameScore(mergedMember, gameRank, game.getScore());

        // calculate rating point
        gameRankService.updateRatingScoreAfterGameEndV2(gameRank, game);

        // save point history
        pointHistoryService.savePointHistory(mergedMember, point, PointHistoryType.RANK_GAME);
    }
}
