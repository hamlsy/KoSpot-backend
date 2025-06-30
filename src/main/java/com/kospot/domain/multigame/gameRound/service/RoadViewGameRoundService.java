package com.kospot.domain.multigame.gameRound.service;

import com.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.domain.multigame.game.entity.MultiRoadViewGame;
import com.kospot.domain.multigame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multigame.gameRound.repository.RoadViewGameRoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoadViewGameRoundService {

    private final CoordinateService coordinateService;
    private final RoadViewGameRoundRepository roundRepository;

    public RoadViewGameRound createGameRound(MultiRoadViewGame game, int currentRound) {
        CoordinateNationwide coordinate = (CoordinateNationwide) coordinateService.getRandomNationwideCoordinate();
        RoadViewGameRound gameRound = RoadViewGameRound.createRound(currentRound, coordinate);
        gameRound.setMultiRoadViewGame(game);
        return roundRepository.save(gameRound);
    }

    public void endGameRound(RoadViewGameRound round) {
        round.endRound();
    }

}
