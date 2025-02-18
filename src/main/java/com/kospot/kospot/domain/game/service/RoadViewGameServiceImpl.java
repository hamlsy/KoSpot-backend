package com.kospot.kospot.domain.game.service;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.kospot.domain.game.adaptor.RoadViewGameAdaptor;
import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.dto.response.StartGameResponse;
import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.repository.RoadViewGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RoadViewGameServiceImpl implements RoadViewGameService {

    private final AESService aesService;
    private final CoordinateService coordinateService;
    private final RoadViewGameAdaptor adaptor;
    private final RoadViewGameRepository repository;

    @Override
    public StartGameResponse.RoadView startPracticeGame(String sidoKey){
        Coordinate coordinate = coordinateService.getRandomCoordinateBySido(sidoKey);
        RoadViewGame game = RoadViewGame.create(coordinate, null, GameType.PRACTICE); //todo add member
        repository.save(game);
        //todo refactor
        return StartGameResponse.RoadView.builder()
                .gameId(toEncryptString(game.getId()))
                .targetLat(toEncryptString(game.getTargetLat()))
                .targetLng(toEncryptString(game.getTargetLng()))
                .build();
    }

    @Override
    public EndGameResponse.RoadViewPractice endPracticeGame(EndGameRequest.RoadViewPractice request){
        RoadViewGame game = adaptor.queryById(request.getGameId());
        game.end(request);
        return EndGameResponse.RoadViewPractice.from(game);
    }


    @Override
    public StartGameResponse.RoadView startRankGame() {
        Coordinate coordinate = coordinateService.getRandomNationwideCoordinate();
        RoadViewGame game = RoadViewGame.create(coordinate, null, GameType.RANK); //todo add member
        repository.save(game);
        return StartGameResponse.RoadView.builder()
                .gameId(toEncryptString(game.getId()))
                .targetLat(toEncryptString(game.getTargetLat()))
                .targetLng(toEncryptString(game.getTargetLng()))
                .build();
    }

    private <T> String toEncryptString(T object){
        return aesService.encrypt(String.valueOf(object));
    }
}
