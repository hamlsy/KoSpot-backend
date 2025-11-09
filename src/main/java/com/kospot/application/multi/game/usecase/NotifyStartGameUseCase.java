package com.kospot.application.multi.game.usecase;

import com.kospot.application.multi.game.strategy.MultiGameStartStrategy;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.exception.object.domain.GameHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.websocket.domain.multi.room.service.GameRoomNotificationService;
import com.kospot.presentation.multi.flow.dto.message.RoomGameStartMessage;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multi.game.dto.response.MultiGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class NotifyStartGameUseCase {

    private static final long DEFAULT_COUNTDOWN_MS = 3_000L;

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomNotificationService gameRoomNotificationService;
    private final List<MultiGameStartStrategy> startStrategies; // GameMode와 MatchType별 스타트 컨텍스트를 준비하는 전략 컬렉션

    public MultiGameResponse.StartGame execute(Member host, Long gameRoomId, MultiGameRequest.Start request) {
        GameRoom gameRoom = gameRoomAdaptor.queryByIdFetchHost(gameRoomId);
        gameRoom.start(host);

        GameMode gameMode = resolveGameMode(gameRoom, request);
        PlayerMatchType matchType = resolveMatchType(gameRoom, request);

        request.setGameModeKey(gameMode.name());
        request.setPlayerMatchTypeKey(matchType.name());
        if (request.getTimeLimit() == null) {
            request.setTimeLimit(gameRoom.getTimeLimit());
        }

        MultiGameStartStrategy strategy = startStrategies.stream()
                .filter(it -> it.supports(gameMode, matchType))
                .findFirst()
                .orElseThrow(() -> new GameHandler(ErrorStatus.GAME_TYPE_NOT_FOUND));

        MultiGameStartStrategy.StartGamePreparation preparation =
                strategy.prepare(gameRoom, request, gameMode, matchType);

        MultiGameResponse.StartGame response = preparation.startGame();
        broadcastGameStart(gameRoom.getId().toString(), preparation, response);
        return response;
    }

    private GameMode resolveGameMode(GameRoom gameRoom, MultiGameRequest.Start request) {
        String gameModeKey = request.getGameModeKey();
        if (gameModeKey == null || gameModeKey.isBlank()) {
            return gameRoom.getGameMode();
        }
        return GameMode.fromKey(gameModeKey);
    }

    private PlayerMatchType resolveMatchType(GameRoom gameRoom, MultiGameRequest.Start request) {
        String matchTypeKey = request.getPlayerMatchTypeKey();
        if (matchTypeKey == null || matchTypeKey.isBlank()) {
            return gameRoom.getPlayerMatchType();
        }
        return PlayerMatchType.fromKey(matchTypeKey);
    }

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
                .totalRounds(response.getTotalRounds())
                .roundTimeLimit(response.getRoundTimeLimit())
                .issuedAt(issuedAt)
                .players(response.getPlayers())
                .payload(response.getPayload())
                .build();

        gameRoomNotificationService.broadcastGameStart(roomId, startMessage);
    }
}

