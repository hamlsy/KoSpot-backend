package com.kospot.application.multi.flow;

import com.kospot.application.multi.game.message.LoadingStatusMessage;
import com.kospot.application.multi.round.roadview.NextRoadViewRoundUseCase;
import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.presentation.multi.game.dto.message.LoadingAckMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 게임 전환을 조율하는 서비스
 * 로딩 완료 확인, 타임아웃 처리, 게임 시작/취소 등을 조율한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameTransitionOrchestrator {

    private static final Duration LOADING_TIMEOUT_DURATION = Duration.ofSeconds(10);

    private final LoadingPhaseService loadingPhaseService;
    private final MultiGameFlowScheduler multiGameFlowScheduler;
    private final NextRoadViewRoundUseCase nextRoadViewRoundUseCase;
    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;

    /**
     * 게임 시작 브로드캐스트 직후 로딩 단계를 시작한다.
     *
     * @param roomId 방 ID
     * @param gameId 게임 ID
     */
    public void initializeLoadingPhase(String roomId, Long gameId) {
        loadingPhaseService.initializeLoadingPhase(roomId, gameId);

        multiGameFlowScheduler.schedule(roomId, MultiGameFlowScheduler.FlowTaskType.LOADING_TIMEOUT,
                LOADING_TIMEOUT_DURATION, () -> handleLoadingTimeout(roomId));
        log.info("Scheduled loading timeout - RoomId: {}, GameId: {}", roomId, gameId);
    }

    /**
     * 플레이어가 로딩 완료를 알리면 처리한다.
     *
     * @param roomId         방 ID
     * @param message        로딩 ACK 메시지
     * @param headerAccessor WebSocket 헤더
     */
    public void handleLoadingAck(String roomId, LoadingAckMessage message,
                                  SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal principal = WebSocketMemberPrincipal.getPrincipal(headerAccessor);
        Long memberId = principal != null ? principal.getMemberId() : null;

        if (memberId == null) {
            log.warn("Missing member id in loading ack - RoomId: {}", roomId);
            return;
        }

        Long acknowledgedAt = message.getClientTimestamp() != null
                ? message.getClientTimestamp()
                : System.currentTimeMillis();

        loadingPhaseService.markPlayerReady(roomId, message.getRoundId(), memberId, acknowledgedAt);

        LoadingStatusMessage statusMessage = loadingPhaseService.buildLoadingStatusMessage(roomId);
        loadingPhaseService.broadcastLoadingStatus(roomId, statusMessage);

        if (statusMessage.isAllArrived()) {
            onAllPlayersArrived(roomId, message.getRoundId());
        }
    }

    /**
     * 모든 플레이어가 도착했을 때 게임을 시작한다.
     */
    private void onAllPlayersArrived(String roomId, Long roundId) {
        multiGameFlowScheduler.cancel(roomId, MultiGameFlowScheduler.FlowTaskType.LOADING_TIMEOUT);
        Long currentGameId = loadingPhaseService.getCurrentGameId(roomId);

        if (currentGameId == null) {
            log.warn("Current game id not found for room - RoomId: {}", roomId);
            return;
        }

        log.info("All players arrived. Starting game - RoomId: {}, GameId: {}", roomId, currentGameId);
        loadingPhaseService.cleanupLoadingState(roomId);

        try {
            Long numericRoomId = Long.parseLong(roomId);
            nextRoadViewRoundUseCase.executeInitial(numericRoomId, currentGameId);
        } catch (NumberFormatException e) {
            log.error("Failed to parse room id for starting game - RoomId: {}, GameId: {}",
                    roomId, currentGameId, e);
        }
    }

    /**
     * 로딩 타임아웃이 발생하면 게임을 취소한다.
     */
    @Transactional
    void handleLoadingTimeout(String roomId) {
        LoadingStatusMessage statusMessage = loadingPhaseService.buildLoadingStatusMessage(roomId);

        if (statusMessage.isAllArrived()) {
            log.info("Timeout fired but all players already arrived - RoomId: {}", roomId);
            return;
        }

        loadingPhaseService.broadcastLoadingStatus(roomId, statusMessage);
        Long currentGameId = loadingPhaseService.getCurrentGameId(roomId);

        if (currentGameId == null) {
            log.warn("Timeout triggered without current game id - RoomId: {}", roomId);
            return;
        }

        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(currentGameId);
        game.cancelGame();
        loadingPhaseService.cleanupLoadingState(roomId);

        log.warn("Loading timeout reached. Game cancelled - RoomId: {}, GameId: {}", roomId, currentGameId);
    }
}
