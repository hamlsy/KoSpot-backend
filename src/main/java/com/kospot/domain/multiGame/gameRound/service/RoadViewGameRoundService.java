package com.kospot.domain.multiGame.gameRound.service;

import com.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.domain.multiGame.game.entity.MultiRoadViewGame;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multiGame.gameRound.repository.RoadViewGameRoundRepository;
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
