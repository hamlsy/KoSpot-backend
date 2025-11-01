package com.kospot.application.game.roadview.practice.event;


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
        // persist member 조회
        Member persistMember = memberAdaptor.queryById(member.getId());

        // 포인트 계산 및 지급
        int point = PointCalculator.getPracticePoint(game.getScore());
        pointService.addPoint(persistMember, point);

        // 포인트 히스토리 저장
        pointHistoryService.savePointHistory(persistMember, point, PointHistoryType.PRACTICE_GAME);
    }
}
