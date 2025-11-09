package com.kospot.domain.multi.round.service;

import com.kospot.domain.coordinate.adaptor.CoordinateAdaptor;
import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.repository.RoadViewGameRoundRepository;
import com.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.infrastructure.exception.object.domain.GameRoundHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoadViewGameRoundService {

    private final CoordinateAdaptor coordinateAdaptor;
    private final CoordinateService coordinateService;
    private final RoadViewGameRoundRepository roundRepository;

    public RoadViewGameRound createGameRound(MultiRoadViewGame game, List<Long> playerIds) {
        Coordinate coordinate = fetchRandomCoordinate();
        RoadViewGameRound gameRound = RoadViewGameRound.createRound(game.getCurrentRound(), coordinate,
                game.getTimeLimit(), playerIds);
        gameRound.setMultiRoadViewGame(game);
        return roundRepository.save(gameRound);
    }

    public RoadViewGameRound reissueRound(RoadViewGameRound round, List<Long> playerIds) {
        Coordinate currentCoordinate = round.getTargetCoordinate();
        if (currentCoordinate != null) {
            coordinateService.invalidateCoordinate(currentCoordinate);
        }

        Coordinate newCoordinate = fetchRandomCoordinate();
        round.reassignCoordinate(newCoordinate);
        if (playerIds != null) {
            round.reassignPlayerIds(playerIds);
        }
        return round;
    }

    public void endGameRound(RoadViewGameRound round) {
        boolean finished = round.finishRound();
        if (!finished) {
            log.warn("Round already finished - RoundId: {}", round.getId());
        }
    }

    public RoadViewGameRound getRound(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new GameRoundHandler(ErrorStatus.GAME_ROUND_NOT_FOUND));
    }

    private Coordinate fetchRandomCoordinate() {
        Coordinate coordinate = coordinateAdaptor.getRandomCoordinate();
        if (coordinate == null) {
            throw new GameRoundHandler(ErrorStatus.COORDINATE_NOT_FOUND);
        }
        return coordinate;
    }
}
