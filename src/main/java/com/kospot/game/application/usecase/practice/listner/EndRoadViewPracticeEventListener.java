package com.kospot.game.application.usecase.practice.listner;

import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.domain.event.RoadViewPracticeEvent;
import com.kospot.game.domain.vo.GameMode;
import com.kospot.game.domain.vo.GameType;
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
public class EndRoadViewPracticeEventListener {

    private final MemberStatisticService memberStatisticService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGameEnd(RoadViewPracticeEvent event){
        try{
            Member member = event.getMember();
            RoadViewGame game = event.getRoadViewGame();

//            updatePointEvent.updatePoint(member, game); // 연습모드 포인트 미지급 처리
            memberStatisticService.updateSingleGameStatistic(member, GameMode.ROADVIEW, GameType.PRACTICE, game.getScore(), game.getEndedAt());
        }catch (Exception e){
            throw new EventHandler(ErrorStatus.EVENT_GAME_END_ERROR);
        }
    }
}
