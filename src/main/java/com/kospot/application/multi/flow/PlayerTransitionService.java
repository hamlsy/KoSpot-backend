package com.kospot.application.multi.flow;

import com.kospot.application.multi.game.message.LoadingStatusMessage;
import com.kospot.application.multi.game.usecase.StartRoadViewSoloGameUseCase;
import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.redis.domain.multi.game.service.MultiGameRedisService;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.domain.multi.game.service.GameNotificationService;
import com.kospot.presentation.multi.game.dto.message.LoadingAckMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerTransitionService {

    private static final long INITIAL_ROUND_ID = 0L;
    private static final Duration LOADING_TIMEOUT_DURATION = Duration.ofSeconds(10);

    private final GameRoomRedisService gameRoomRedisService;
    private final MultiGameRedisService multiGameRedisService;
    private final GameNotificationService gameNotificationService;
    private final MultiGameFlowScheduler multiGameFlowScheduler;
    private final StartRoadViewSoloGameUseCase startRoadViewSoloGameUseCase;
    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;

    /**
     * 게임 시작 브로드캐스트 직후 로딩 상태를 초기화하고 타임아웃 타이머를 건다.
     */
    public void initializeLoadingPhase(String roomId, Long gameId) {
        multiGameRedisService.resetLoadingStatus(roomId);
        multiGameRedisService.setCurrentGameId(roomId, gameId);

        multiGameFlowScheduler.schedule(roomId, MultiGameFlowScheduler.FlowTaskType.LOADING_TIMEOUT,
                LOADING_TIMEOUT_DURATION, () -> handleLoadingTimeout(roomId));
        log.info("Initialized loading phase - RoomId: {}, GameId: {}", roomId, gameId);
    }

    /**
     * 플레이어가 준비 완료를 알리면 Redis에 기록하고 전체 로딩 상태 방송
     */
    public void handleLoadingAck(String roomId, LoadingAckMessage message, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal principal = WebSocketMemberPrincipal.getPrincipal(headerAccessor);
        Long memberId = principal != null ? principal.getMemberId() : null;
        Long roundId = message.getRoundId() != null ? message.getRoundId() : INITIAL_ROUND_ID;
        if (memberId == null) {
            log.warn("Missing member id in loading ack - RoomId: {}", roomId);
            return;
        }

        Long acknowledgedAt = message.getClientTimestamp() != null ? message.getClientTimestamp() : System.currentTimeMillis();
        multiGameRedisService.markPlayerLoadingReady(roomId, roundId, memberId, acknowledgedAt);

        LoadingStatusMessage statusMessage = buildLoadingStatusMessage(roomId);
        gameNotificationService.broadcastLoadingStatus(roomId, statusMessage);

        if (statusMessage.isAllArrived()) {
            onAllPlayersArrived(roomId, roundId);
        }
    }

    /**
     * 현재 라운드 기준으로 각 플레이어의 로딩 도착 여부 계산
     */
    private LoadingStatusMessage buildLoadingStatusMessage(String roomId) {
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(roomId);
        Map<Long, Long> statusMap = multiGameRedisService.getPlayerLoadingStatus(roomId);

        List<LoadingStatusMessage.MemberLoadingState> states = players.stream()
                .map(player -> {
                    Long acknowledgedAt = statusMap.get(player.getMemberId());
                    return LoadingStatusMessage.MemberLoadingState.builder()
                            .memberId(player.getMemberId())
                            .arrived(acknowledgedAt != null)
                            .acknowledgedAt(acknowledgedAt)
                            .build();
                })
                .toList();

        boolean allArrived = states.stream().allMatch(LoadingStatusMessage.MemberLoadingState::isArrived);

        return LoadingStatusMessage.builder()
                .players(states)
                .allArrived(allArrived)
                .build();
    }

    /**
     * 모든 플레이어가 도착했을 때 로딩 타이머를 취소 및 실 게임 시작
     */
    private void onAllPlayersArrived(String roomId, Long roundId) {
        multiGameFlowScheduler.cancel(roomId, MultiGameFlowScheduler.FlowTaskType.LOADING_TIMEOUT);
        Long currentGameId = multiGameRedisService.getCurrentGameId(roomId);
        if (currentGameId == null) {
            log.warn("Current game id not found for room - RoomId: {}", roomId);
            return;
        }

        log.info("All players arrived. Starting game - RoomId: {}, GameId: {}", roomId, currentGameId);
        multiGameRedisService.resetLoadingStatus(roomId);
        multiGameRedisService.clearCurrentGameId(roomId);
        try {
            Long numericRoomId = Long.parseLong(roomId);
            startRoadViewSoloGameUseCase.execute(numericRoomId, currentGameId);
        } catch (NumberFormatException e) {
            log.error("Failed to parse room id for starting game - RoomId: {}, GameId: {}", roomId, currentGameId, e);
        }
    }

    @Transactional
    /**
     * 로딩 타임아웃이 발생하면 현재 상태를 알리고 게임을 취소한다.
     */
    void handleLoadingTimeout(String roomId) {
        LoadingStatusMessage statusMessage = buildLoadingStatusMessage(roomId);
        if (statusMessage.isAllArrived()) {
            log.info("Timeout fired but all players already arrived - RoomId: {}", roomId);
            return;
        }

        gameNotificationService.broadcastLoadingStatus(roomId, statusMessage);
        Long currentGameId = multiGameRedisService.getCurrentGameId(roomId);
        if (currentGameId == null) {
            log.warn("Timeout triggered without current game id - RoomId: {}", roomId);
            return;
        }

        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(currentGameId);
        game.cancelGame();
        multiGameRedisService.resetLoadingStatus(roomId);
        multiGameRedisService.clearCurrentGameId(roomId);
        log.warn("Loading timeout reached. Game cancelled - RoomId: {}, GameId: {}", roomId, currentGameId);
    }
}
