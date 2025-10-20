package com.kospot.domain.multi.round.service;

import com.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.repository.RoadViewGameRoundRepository;
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

    private final CoordinateService coordinateService;
    private final RoadViewGameRoundRepository roundRepository;

    public RoadViewGameRound createGameRound(MultiRoadViewGame game, List<Long> playerIds) {
        CoordinateNationwide coordinate = (CoordinateNationwide) coordinateService.getRandomNationwideCoordinate();
        RoadViewGameRound gameRound = RoadViewGameRound.createRound(game.getCurrentRound(), coordinate,
                game.getTimeLimit(), playerIds);
        gameRound.setMultiRoadViewGame(game);
        gameRound.startRound();
        return roundRepository.save(gameRound);
    }

    public void endGameRound(RoadViewGameRound round) {
        boolean finished = round.finishRound();
        if (!finished) {
            log.warn("Round already finished - RoundId: {}", round.getId());
        }
    }

}
