package com.kospot.kospot.domain.game.service;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.kospot.domain.game.repository.RoadViewGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoadViewGameServiceImpl implements GameService {

    private CoordinateService coordinateService;
    private RoadViewGameRepository repository;

    public void startGame(String sidoKey){
        Coordinate coordinate = coordinateService.getRandomCoordinateBySido(sidoKey);

    }

    public void endGame(){

    }

}
