package com.kospot.application.multi.game.listener;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.game.event.MultiGameFinishedEvent;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.point.service.PointHistoryService;
import com.kospot.domain.point.service.PointService;
import com.kospot.domain.point.util.PointCalculator;
import com.kospot.domain.point.vo.PointHistoryType;
import com.kospot.infrastructure.exception.object.domain.EventHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MultiGameFinishedEventListener {

    private final MemberAdaptor memberAdaptor;
    private final PointService pointService;
    private final PointHistoryService pointHistoryService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMultiGameFinished(MultiGameFinishedEvent event) {
        try {
            for (GamePlayer gamePlayer : event.getGamePlayers()) {
                distributePointToPlayer(gamePlayer);
            }
            
            log.info("✅ Successfully distributed points to all players for gameId: {}", event.getGameId());
        } catch (Exception e) {
            log.error("❌ Failed to distribute points for gameId: {}", event.getGameId(), e);
            throw new EventHandler(ErrorStatus.EVENT_GAME_END_ERROR);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void distributePointToPlayer(GamePlayer gamePlayer) {
        try {
            // 멤버 조회
            Member member = memberAdaptor.queryById(gamePlayer.getMemberId());
            
            // 포인트 계산 (roundRank가 최종 순위)
            int finalRank = gamePlayer.getRoundRank() != null ? gamePlayer.getRoundRank() : 999;
            int earnedPoint = PointCalculator.getMultiGamePoint(finalRank, gamePlayer.getTotalScore());
            
            // 포인트 지급
            pointService.addPoint(member, earnedPoint);
            
            // 포인트 히스토리 저장
            pointHistoryService.savePointHistory(member, earnedPoint, PointHistoryType.MULTI_GAME);
            
            log.info("💰 Point distributed - MemberId: {}, Rank: {}, Score: {}, Point: {}", 
                    member.getId(), finalRank, gamePlayer.getTotalScore(), earnedPoint);
                    
        } catch (Exception e) {
            log.error("❌ Failed to distribute point to player: {}", gamePlayer.getId(), e);
            // 개별 실패는 다른 플레이어에게 영향 없도록 예외를 먹음
        }
    }
}

