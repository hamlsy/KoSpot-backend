package com.kospot.application.multi.flow;

import com.kospot.application.multi.game.message.LoadingStatusMessage;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.redis.domain.multi.game.service.MultiGameRedisService;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.infrastructure.websocket.domain.multi.game.service.GameNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 플레이어 로딩 상태를 관리하는 서비스
 * 로딩 상태 초기화, 플레이어 도착 기록, 상태 조회를 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoadingPhaseService {

    private static final long INITIAL_ROUND_ID = 0L;

    private final GameRoomRedisService gameRoomRedisService;
    private final MultiGameRedisService multiGameRedisService;
    private final GameNotificationService gameNotificationService;

    /**
     * 로딩 단계를 초기화한다.
     *
     * @param roomId 방 ID
     * @param gameId 게임 ID
     */
    public void initializeLoadingPhase(String roomId, Long gameId) {
        multiGameRedisService.resetLoadingStatus(roomId);
        multiGameRedisService.setCurrentGameId(roomId, gameId);
        log.info("Initialized loading phase - RoomId: {}, GameId: {}", roomId, gameId);
    }

    /**
     * 플레이어의 로딩 완료를 기록한다.
     *
     * @param roomId         방 ID
     * @param roundId        라운드 ID (null이면 초기 라운드)
     * @param memberId       멤버 ID
     * @param acknowledgedAt 도착 시간
     */
    public void markPlayerReady(String roomId, Long roundId, Long memberId, Long acknowledgedAt) {
        Long resolvedRoundId = roundId != null ? roundId : INITIAL_ROUND_ID;
        multiGameRedisService.markPlayerLoadingReady(roomId, resolvedRoundId, memberId, acknowledgedAt);
    }

    /**
     * 현재 로딩 상태를 조회하여 메시지를 생성한다.
     *
     * @param roomId 방 ID
     * @return 로딩 상태 메시지
     */
    public LoadingStatusMessage buildLoadingStatusMessage(String roomId) {
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
     * 로딩 상태를 브로드캐스트한다.
     *
     * @param roomId        방 ID
     * @param statusMessage 로딩 상태 메시지
     */
    public void broadcastLoadingStatus(String roomId, LoadingStatusMessage statusMessage) {
        gameNotificationService.broadcastLoadingStatus(roomId, statusMessage);
    }

    /**
     * 현재 게임 ID를 조회한다.
     *
     * @param roomId 방 ID
     * @return 게임 ID
     */
    public Long getCurrentGameId(String roomId) {
        return multiGameRedisService.getCurrentGameId(roomId);
    }

    /**
     * 로딩 상태를 정리한다.
     *
     * @param roomId 방 ID
     */
    public void cleanupLoadingState(String roomId) {
        multiGameRedisService.resetLoadingStatus(roomId);
        multiGameRedisService.clearCurrentGameId(roomId);
    }
}
