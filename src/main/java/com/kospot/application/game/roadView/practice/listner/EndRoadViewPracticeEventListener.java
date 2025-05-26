package com.kospot.application.game.roadView.practice.listner;

import com.kospot.application.game.roadView.practice.event.UpdatePointEvent;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.event.RoadViewPracticeEvent;
import com.kospot.domain.member.entity.Member;
import com.kospot.global.exception.object.domain.EventHandler;
import com.kospot.global.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EndRoadViewPracticeEventListener {

    private final UpdatePointEvent updatePointEvent;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGameEnd(RoadViewPracticeEvent event){
        try{
            Member member = event.getMember();
            RoadViewGame game = event.getRoadViewGame();

            updatePointEvent.updatePoint(member, game);
        }catch (Exception e){
            throw new EventHandler(ErrorStatus.EVENT_GAME_END_ERROR);
        }
    }
}
