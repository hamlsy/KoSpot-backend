package com.kospot.domain.mvp.service;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.domain.mvp.adaptor.DailyMvpAdaptor;
import com.kospot.domain.mvp.entity.DailyMvp;
import com.kospot.domain.mvp.event.MvpRewardGrantedEvent;
import com.kospot.domain.point.service.PointHistoryService;
import com.kospot.domain.point.service.PointService;
import com.kospot.domain.point.vo.PointHistoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyMvpRewardService {

    private final DailyMvpAdaptor dailyMvpAdaptor;
    private final MemberAdaptor memberAdaptor;
    private final PointService pointService;
    private final PointHistoryService pointHistoryService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public int rewardUnprocessedUpTo(LocalDate targetDate) {
        List<DailyMvp> unrewarded = dailyMvpAdaptor.queryUnrewardedByDateLessThanEqual(targetDate);
        int rewardedCount = 0;

        for (DailyMvp item : unrewarded) {
            DailyMvp locked = dailyMvpAdaptor.queryByDateForUpdate(item.getMvpDate())
                    .orElse(null);

            if (locked == null || locked.isRewardGranted()) {
                continue;
            }

            Member member = memberAdaptor.queryById(locked.getMemberId());
            pointService.addPoint(member, locked.getRewardPoint());
            pointHistoryService.savePointHistory(member, locked.getRewardPoint(), PointHistoryType.MVP_REWARD);

            locked.grantReward(LocalDateTime.now());
            rewardedCount++;

            eventPublisher.publishEvent(new MvpRewardGrantedEvent(
                    locked.getMemberId(),
                    locked.getMvpDate(),
                    locked.getRoadViewGameId(),
                    locked.getRewardPoint()
            ));
        }

        log.info("Daily MVP rewards processed. targetDate={}, rewardedCount={}", targetDate, rewardedCount);
        return rewardedCount;
    }
}
