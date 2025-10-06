package com.kospot.application.multi.game;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.round.entity.BaseGameRound;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.service.RoadViewGameRoundService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.multi.timer.service.GameTimerService;
import com.kospot.infrastructure.websocket.domain.multi.timer.vo.TimerCommand;
import com.kospot.presentation.multigame.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multigame.game.dto.response.MultiRoadViewGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class NextRoadViewRoundUseCase {

    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GameTimerService gameTimerService;

    public MultiRoadViewGameResponse.NextRound execute(Long gameRoomId, Long multiGameId, int currentRound, Integer timeLimit) {
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(multiGameId);
        game.moveToNextRound();

        //todo playerIds를 redis에서 가져오기
        // List<Long> playerIds = gamePlayerRedisRepository.findPlayerIdsByGameId(multiGameId);
        RoadViewGameRound round = roadViewGameRoundService.createGameRound(game, currentRound, timeLimit, null);

        TimerCommand command = createTimerCommand(gameRoomId, game, round);

        //타이머 시작
        gameTimerService.startRoundTimer(command);
        return MultiRoadViewGameResponse.NextRound.from(game, round);
    }

    private TimerCommand createTimerCommand(Long gameRoomId, MultiRoadViewGame game,
                                            BaseGameRound round) {
        return TimerCommand.builder()
                .round(round)
                .gameRoomId(gameRoomId.toString())
                .gameId(game.getId().toString())
                .gameMode(GameMode.ROADVIEW)
                .matchType(PlayerMatchType.SOLO)
                .build();
    }

}
