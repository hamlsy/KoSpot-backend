package com.kospot.kospot.application.game.roadView.rank.listener;

import com.kospot.kospot.application.game.roadView.rank.event.UpdatePointAndRankEvent;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.event.RoadViewGameEvent;
import com.kospot.kospot.domain.member.entity.Member;
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
    public void handleGameEnd(RoadViewGameEvent event){
        try{
            Member member = event.getMember();
            RoadViewGame game = event.getRoadViewGame();
            updatePointAndRankEvent.updatePointAndRank(member, game);
        }catch (Exception e){
            //todo exception handling
        }
    }

}
