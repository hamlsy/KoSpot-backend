package com.kospot.game.application.usecase.listener;


import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.domain.event.RoadViewRankEvent;
import com.kospot.game.domain.event.UpdatePointAndRankEvent;
import com.kospot.game.domain.vo.GameMode;
import com.kospot.game.domain.vo.GameType;
import com.kospot.gamerank.domain.entity.GameRank;
import com.kospot.member.domain.entity.Member;
import com.kospot.statistic.application.service.MemberStatisticService;
import com.kospot.common.exception.object.domain.EventHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EndRoadViewRankEventListener {

    private final UpdatePointAndRankEvent updatePointAndRankEvent;
    private final MemberStatisticService memberStatisticService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGameEnd(RoadViewRankEvent event){
        try{
            Member member = event.getMember();
            RoadViewGame game = event.getRoadViewGame();
            GameRank gameRank = event.getGameRank();

            updatePointAndRankEvent.updatePointAndRank(member, game, gameRank.getRankTier());
            memberStatisticService.updateSingleGameStatistic(member, GameMode.ROADVIEW, GameType.RANK, game.getScore(), game.getEndedAt());
        }catch (Exception e){
            throw new EventHandler(ErrorStatus.EVENT_GAME_END_ERROR);
        }
    }

}