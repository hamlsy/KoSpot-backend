package com.kospot.multi.game.application.service;

import com.kospot.multi.common.flow.MultiGameFlowScheduler;
import com.kospot.multi.game.application.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.multi.game.domain.entity.MultiRoadViewGame;
import com.kospot.multi.player.application.adaptor.GamePlayerAdaptor;
import com.kospot.multi.game.infrastructure.websocket.service.GameNotificationService;
import com.kospot.multi.timer.infrastructure.websocket.service.GameTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CancelMultiGameService {

    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final GamePlayerAdaptor gamePlayerAdaptor;
    private final MultiGameFlowScheduler multiGameFlowScheduler;
    private final GameTimerService gameTimerService;
    private final GameNotificationService gameNotificationService;

    /**
     * 활성 플레이어가 없어서 게임을 취소합니다. (라운드 경계 안전장치)
     * - 게임 상태를 CANCELLED로 변경
     * - 스케줄러/타이머 태스크 취소
     * - 클라이언트에 게임 취소 알림 브로드캐스트
     * 
     * 참고: 모든 플레이어가 퇴장하면 방이 삭제되므로(DELETE_ROOM),
     * 이 메서드는 주로 라운드 경계에서 비동기 콜백 실행 시 안전장치로 사용됩니다.
     * 방 상태 변경은 하지 않습니다 (방이 이미 삭제되었을 수 있음).
     */
    public void cancelDueToNoPlayers(Long roomId, Long gameId) {
        String roomKey = roomId.toString();

        // 1. 게임 상태 변경
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(gameId);
        if (game.isCancelled() || game.isFinished()) {
            log.info("Game already cancelled or finished - RoomId: {}, GameId: {}", roomId, gameId);
            return;
        }
        game.cancelGame();

        // 2. 스케줄러 태스크 취소
        multiGameFlowScheduler.cancelAll(roomKey);
        gameTimerService.cancelAllForGame(roomKey, gameId);

        // 3. 클라이언트에 게임 취소 알림
        gameNotificationService.broadcastGameCancelled(roomKey, gameId, "ALL_PLAYERS_LEFT");

        log.info("Game cancelled due to no active players - RoomId: {}, GameId: {}", roomId, gameId);
    }

    /**
     * 활성 플레이어 수를 확인하고 0명이면 게임을 취소합니다.
     * @return 게임이 취소되었으면 true
     */
    public boolean cancelIfNoActivePlayers(Long roomId, Long gameId) {
        int activePlayerCount = gamePlayerAdaptor.countActivePlayersByGameId(gameId);
        if (activePlayerCount == 0) {
            cancelDueToNoPlayers(roomId, gameId);
            return true;
        }
        return false;
    }

    /**
     * 방 삭제 시 진행 중인 게임을 취소합니다.
     * 방이 삭제되므로 방 상태 변경 및 로비 알림은 생략합니다.
     */
    public void cancelGameOnRoomDeletion(Long roomId) {
        String roomKey = roomId.toString();

        // 진행 중인 게임 조회
        java.util.Optional<MultiRoadViewGame> inProgressGame = 
                multiRoadViewGameAdaptor.findInProgressByGameRoomId(roomId);
        
        if (inProgressGame.isEmpty()) {
            log.debug("No in-progress game to cancel on room deletion - RoomId: {}", roomId);
            return;
        }

        MultiRoadViewGame game = inProgressGame.get();
        Long gameId = game.getId();

        // 1. 게임 상태 변경
        game.cancelGame();

        // 2. 스케줄러 태스크 취소
        multiGameFlowScheduler.cancelAll(roomKey);
        gameTimerService.cancelAllForGame(roomKey, gameId);

        // 3. 클라이언트에 게임 취소 알림 (아직 연결된 클라이언트가 있을 수 있음)
        gameNotificationService.broadcastGameCancelled(roomKey, gameId, "ROOM_DELETED");

        log.info("Game cancelled due to room deletion - RoomId: {}, GameId: {}", roomId, gameId);
    }
}

