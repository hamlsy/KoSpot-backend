package com.kospot.application.game.roadview.rank.listener;

import com.kospot.application.game.roadview.rank.event.UpdatePointAndRankEvent;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.event.RoadViewRankEvent;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.object.domain.EventHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EndRoadViewRankEventListener {

    private final UpdatePointAndRankEvent updatePointAndRankEvent;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGameEnd(RoadViewRankEvent event){
        try{
            Member member = event.getMember();
            RoadViewGame game = event.getRoadViewGame();
            GameRank gameRank = event.getGameRank();

            updatePointAndRankEvent.updatePointAndRank(member, game, gameRank.getRankTier());
        }catch (Exception e){
            throw new EventHandler(ErrorStatus.EVENT_GAME_END_ERROR);
        }
    }

}
