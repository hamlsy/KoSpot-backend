package com.kospot.mvp.application.listener;

import com.kospot.game.domain.event.RoadViewRankEvent;
import com.kospot.game.domain.vo.GameStatus;
import com.kospot.game.domain.vo.GameType;
import com.kospot.mvp.application.service.DailyMvpIncrementalAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyMvpIncrementalAggregationEventListener {

    private final DailyMvpIncrementalAggregationService dailyMvpIncrementalAggregationService;

    @Value("${mvp.incremental.enabled:false}")
    private boolean incrementalEnabled;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RoadViewRankEvent event) {
        if (!incrementalEnabled) {
            return;
        }

        if (event.getRoadViewGame().getGameType() != GameType.RANK || event.getRoadViewGame().getGameStatus() != GameStatus.COMPLETED) {
            return;
        }

        boolean updated = dailyMvpIncrementalAggregationService.aggregate(event.getRoadViewGame(), event.getGameRank());
        if (updated) {
            log.info("Incremental daily MVP updated. gameId={}", event.getRoadViewGame().getId());
        }
    }
}
