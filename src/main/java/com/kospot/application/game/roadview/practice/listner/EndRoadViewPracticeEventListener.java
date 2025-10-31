package com.kospot.application.game.roadview.practice.listner;

import com.kospot.application.game.roadview.practice.event.UpdatePointEvent;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.event.RoadViewPracticeEvent;
import com.kospot.domain.game.vo.GameType;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.service.MemberStatisticService;
import com.kospot.infrastructure.exception.object.domain.EventHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EndRoadViewPracticeEventListener {

    private final UpdatePointEvent updatePointEvent;
    private final MemberStatisticService memberStatisticService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGameEnd(RoadViewPracticeEvent event){
        try{
            Member member = event.getMember();
            RoadViewGame game = event.getRoadViewGame();

            updatePointEvent.updatePoint(member, game);
            memberStatisticService.updateSingleGameStatistic(member, GameType.PRACTICE, game.getScore(), game.getEndedAt());
        }catch (Exception e){
            throw new EventHandler(ErrorStatus.EVENT_GAME_END_ERROR);
        }
    }
}
