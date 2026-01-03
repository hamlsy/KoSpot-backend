package com.kospot.application.multi.game.usecase;

import com.kospot.application.multi.flow.GameTransitionOrchestrator;
import com.kospot.application.multi.game.strategy.MultiGameStartStrategy;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.exception.object.domain.GameHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.websocket.domain.multi.game.service.GameNotificationService;
import com.kospot.presentation.multi.flow.dto.message.RoomGameStartMessage;
import com.kospot.presentation.multi.game.dto.response.MultiGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게임 시작 알림 및 초기화를 처리하는 UseCase
 * 방장이 게임 시작을 요청하면 전략을 통해 게임을 생성하고 로딩 단계를 시작한다.
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class NotifyStartGameUseCase {

    private static final long DEFAULT_COUNTDOWN_MS = 3_000L;

    // Domain Services
    private final GameRoomService gameRoomService;
    private final GameRoomAdaptor gameRoomAdaptor;

    // Strategy Collection
    private final List<MultiGameStartStrategy> startStrategies;

    // Orchestrator
    private final GameTransitionOrchestrator gameTransitionOrchestrator;

    // Infrastructure Services (직접 사용)
    private final GameNotificationService gameNotificationService;

    /**
     * 방장이 게임 시작을 요청하면 모드별 전략으로 컨텍스트를 만들고 로딩 단계를 연다.
     */
    public MultiGameResponse.StartGame execute(Member host, Long gameRoomId) {
        if (gameRoomId == null) {
            throw new GameHandler(ErrorStatus.GAME_ROOM_NOT_FOUND);
        }
        GameRoom gameRoom = gameRoomAdaptor.queryByIdFetchHost(gameRoomId);
        gameRoomService.markGameRoomAsInGame(gameRoom, host);

        GameMode gameMode = gameRoom.getGameMode();
        PlayerMatchType matchType = gameRoom.getPlayerMatchType();

        MultiGameStartStrategy strategy = findStrategy(gameMode, matchType);

        MultiGameStartStrategy.StartGamePreparation preparation =
                strategy.prepare(gameRoom, gameMode, matchType);

        MultiGameResponse.StartGame response = preparation.startGame();
        String roomKey = gameRoom.getId().toString();

        // 게임 시작 브로드캐스트
        broadcastGameStart(roomKey, preparation, response);

        // 로딩 단계 시작
        gameTransitionOrchestrator.initializeLoadingPhase(roomKey, response.getGameId());

        return response;
    }

    private MultiGameStartStrategy findStrategy(GameMode gameMode, PlayerMatchType matchType) {
        return startStrategies.stream()
                .filter(it -> it.supports(gameMode, matchType))
                .findFirst()
                .orElseThrow(() -> new GameHandler(ErrorStatus.GAME_TYPE_NOT_FOUND));
    }

    /**
     * 게임 시작 브로드캐스트 메시지를 구성해 프론트로 송신한다.
     */
    private void broadcastGameStart(String roomId,
                                    MultiGameStartStrategy.StartGamePreparation preparation,
                                    MultiGameResponse.StartGame response) {
        long countdownMs = preparation.countdownMs() != null ? preparation.countdownMs() : DEFAULT_COUNTDOWN_MS;
        long issuedAt = System.currentTimeMillis();
        long deadlineTs = issuedAt + countdownMs;

        RoomGameStartMessage startMessage = RoomGameStartMessage.builder()
                .target(preparation.targetRoute())
                .gameMode(response.getGameMode())
                .matchType(response.getMatchType())
                .gameId(response.getGameId())
                .roundId(response.getRoundId())
                .totalRounds(response.getTotalRounds())
                .currentRound(response.getCurrentRound())
                .roundTimeLimit(response.getRoundTimeLimit())
                .countdownMs(countdownMs)
                .deadlineTs(deadlineTs)
                .issuedAt(issuedAt)
                .players(response.getPlayers())
                .payload(response.getPayload())
                .build();

        gameNotificationService.broadcastGameStart(roomId, startMessage);
    }
}
