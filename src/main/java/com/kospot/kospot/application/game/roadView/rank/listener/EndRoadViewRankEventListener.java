package com.kospot.kospot.application.game.roadView.rank.listener;

import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.event.RoadViewGameEvent;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EndRoadViewRankEventListener {

    private final RoadViewGameService roadViewGameService;
    private final PointService pointService;
    private final GameRankAdaptor gameRankAdaptor;
    private final PointHistoryService pointHistoryService;
    private final GameRankService gameRankService;
    private final EntityManager em;
    private final GameRankRepository gameRankRepository;

    private final UpdatePointAndRankEvent updatePointAndRankEvent;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGameEnd(RoadViewGameEvent event){
        try{
            Member member = event.getMember();
            RoadViewGame game = event.getRoadViewGame();
//            GameRank gameRank = event.getGameRank();
            updatePointAndRankEvent.updatePointAndRank(member, game);
//            updatePointAndRank(member, game, gameRank);
        }catch (Exception e){
            //todo exception handling
        }
    }

//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void updatePointAndRank(Member member, RoadViewGame game){
//        // earn point
//        GameRank gameRank = gameRankAdaptor.queryByMemberAndGameType(member, GameType.ROADVIEW);
//        int point = pointService.addPointByRankGameScore(member, gameRank, game.getScore());
//
//        // calculate rating point
//        gameRankService.updateRatingScoreAfterGameEndV2(gameRank, game);
//
//        // save point history
//        pointHistoryService.savePointHistory(member, point, PointHistoryType.RANK_GAME);
//    }

}
