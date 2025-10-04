package com.kospot.domain.multigame.round.service;

import com.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.domain.multigame.game.entity.MultiRoadViewGame;
import com.kospot.domain.multigame.round.entity.RoadViewGameRound;
import com.kospot.domain.multigame.round.repository.RoadViewGameRoundRepository;
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

    public RoadViewGameRound createGameRound(MultiRoadViewGame game, int currentRound, Integer timeLimit , List<Long> playerIds) {
        CoordinateNationwide coordinate = (CoordinateNationwide) coordinateService.getRandomNationwideCoordinate();
        RoadViewGameRound gameRound = RoadViewGameRound.createRound(currentRound, coordinate, timeLimit, playerIds);
        gameRound.setMultiRoadViewGame(game);
        gameRound.startRound();
        return roundRepository.save(gameRound);
    }

    public void endGameRound(RoadViewGameRound round) {
        round.finishRound();
    }

}
