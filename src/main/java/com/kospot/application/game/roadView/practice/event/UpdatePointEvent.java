package com.kospot.application.game.roadView.practice.event;


import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.point.vo.PointHistoryType;
import com.kospot.domain.point.service.PointHistoryService;
import com.kospot.domain.point.service.PointService;
import com.kospot.domain.point.util.PointCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdatePointEvent {

    private final PointService pointService;
    private final MemberAdaptor memberAdaptor;
    private final PointHistoryService pointHistoryService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePoint(Member member, RoadViewGame game) {
        //earn point
        Member persistMember = memberAdaptor.queryById(member.getId()); // persist

        int point = PointCalculator.getPracticePoint(game.getScore());
        pointService.addPoint(member, point);

        // save point history
        pointHistoryService.savePointHistory(member, point, PointHistoryType.PRACTICE_GAME);

    }
}
