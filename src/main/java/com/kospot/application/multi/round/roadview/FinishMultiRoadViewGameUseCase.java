package com.kospot.application.multi.round.roadview;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.statistic.service.MemberStatisticService;
import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.point.service.PointHistoryService;
import com.kospot.domain.point.service.PointService;
import com.kospot.domain.point.util.PointCalculator;
import com.kospot.domain.point.vo.PointHistoryType;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.multi.round.service.GameRoundNotificationService;
import com.kospot.presentation.multi.game.dto.response.MultiGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class FinishMultiRoadViewGameUseCase {

    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final GamePlayerAdaptor gamePlayerAdaptor;
    private final GameRoundNotificationService gameRoundNotificationService;
    private final PointService pointService;
    private final PointHistoryService pointHistoryService;
    private final MemberStatisticService memberStatisticService;

    public void execute(String gameRoomId, Long gameId) {
        // 1. 게임 종료 처리
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(gameId);
        List<GamePlayer> players = gamePlayerAdaptor.queryByMultiRoadViewGameIdWithMember(gameId);
        game.finishGame();
        
        log.info("🎮 Game finished - gameId: {}, players: {}", gameId, players.size());

        // 2. 포인트 지급
        for (GamePlayer player : players) {
            distributePointToPlayer(player);
        }
        
        // 3. WebSocket 최종 결과 전송
        MultiGameResponse.GameFinalResult finalResult = MultiGameResponse.GameFinalResult.from(gameId, players);
        gameRoundNotificationService.notifyGameFinishedWithResults(gameRoomId, finalResult);

        // 관련 redis 데이터 정리

        log.info("✅ Game completed with point distribution - gameId: {}", gameId);
    }

    private void distributePointToPlayer(GamePlayer player) {
        Member member = player.getMember();
        
        int finalRank = player.getRoundRank() != null ? player.getRoundRank() : 999;
        int earnedPoint = PointCalculator.getMultiGamePoint(finalRank, player.getTotalScore());
        
        pointService.addPoint(member, earnedPoint);
        pointHistoryService.savePointHistory(member, earnedPoint, PointHistoryType.MULTI_GAME);
        memberStatisticService.updateMultiGameStatistic(member, GameMode.ROADVIEW, player.getTotalScore(), player.getRoundRank(), LocalDateTime.now());
        
        log.info("💰 Point distributed - memberId: {}, rank: {}, score: {}, point: {}", 
                member.getId(), finalRank, player.getTotalScore(), earnedPoint);
    }

}