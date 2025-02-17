package com.kospot.kospot.domain.game.service;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.kospot.domain.game.dto.response.StartGameResponse;
import com.kospot.kospot.domain.game.entity.Game;
import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.repository.RoadViewGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoadViewGameServiceImpl implements RoadViewGameService {

    private final CoordinateService coordinateService;
    private final RoadViewGameRepository repository;

    @Override
    public StartGameResponse.RoadView startPracticeGame(String sidoKey){
        Coordinate coordinate = coordinateService.getRandomCoordinateBySido(sidoKey);
        RoadViewGame game = RoadViewGame.create(coordinate, null, GameType.PRACTICE); //todo add member
        return StartGameResponse.RoadView.from(game);
    }

    @Override
    public void endPracticeGame(){

    }

}
