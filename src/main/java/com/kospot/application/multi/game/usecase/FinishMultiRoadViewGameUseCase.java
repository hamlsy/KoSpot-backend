package com.kospot.application.multi.game.usecase;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
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
import com.kospot.infrastructure.websocket.domain.multi.lobby.service.LobbyRoomNotificationService;
import com.kospot.infrastructure.websocket.domain.multi.round.service.GameRoundNotificationService;
import com.kospot.presentation.multi.game.dto.response.MultiGameResponse;
import com.kospot.presentation.multi.game.mapper.MultiGameResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 멀티 로드뷰 게임 종료를 처리하는 UseCase
 * 게임 종료, 포인트 지급, 통계 업데이트, 알림 전송을 조율한다.
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class FinishMultiRoadViewGameUseCase {

    // Domain Adaptors
    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final GamePlayerAdaptor gamePlayerAdaptor;
    private final GameRoomAdaptor gameRoomAdaptor;

    // Domain Services
    private final GameRoomService gameRoomService;
    private final PointService pointService;
    private final PointHistoryService pointHistoryService;
    private final MemberStatisticService memberStatisticService;

    // Mapper
    private final MultiGameResponseMapper responseMapper;

    // Infrastructure Services (직접 사용)
    private final GameRoundNotificationService gameRoundNotificationService;
    private final LobbyRoomNotificationService lobbyRoomNotificationService;

    public void execute(String gameRoomId, Long gameId) {
        // 1. 게임 종료 처리
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(gameId);
        List<GamePlayer> players = gamePlayerAdaptor.queryByMultiRoadViewGameIdWithMember(gameId);
        game.finishGame();

        // 2. 포인트 지급
        for (GamePlayer player : players) {
            distributePointToPlayer(player);
        }

        // 3. WebSocket 최종 결과 전송
        MultiGameResponse.GameFinalResult finalResult = responseMapper.toGameFinalResult(gameId, players);
        gameRoundNotificationService.notifyGameFinishedWithResults(gameRoomId, finalResult);

        // 4. 방 상태 변경
        GameRoom gameRoom = gameRoomAdaptor.queryById(Long.valueOf(gameRoomId));
        gameRoomService.markGameRoomAsWaiting(gameRoom);

        // 5. 로비 알림
        lobbyRoomNotificationService.notifyRoomStatusUpdated(gameRoom.getId(), players.size(), GameRoomStatus.WAITING);

        log.info("Game completed with point distribution - gameId: {}", gameId);
    }

    private void distributePointToPlayer(GamePlayer player) {
        Member member = player.getMember();

        int finalRank = player.getRoundRank() != null ? player.getRoundRank() : 999;
        int earnedPoint = PointCalculator.getMultiGamePoint(finalRank, player.getTotalScore());

        pointService.addPoint(member, earnedPoint);
        pointHistoryService.savePointHistory(member, earnedPoint, PointHistoryType.MULTI_GAME);
        memberStatisticService.updateMultiGameStatistic(
                member, GameMode.ROADVIEW, player.getTotalScore(), player.getRoundRank(), LocalDateTime.now());

        log.debug("Point distributed - memberId: {}, rank: {}, score: {}, point: {}",
                member.getId(), finalRank, player.getTotalScore(), earnedPoint);
    }
}
