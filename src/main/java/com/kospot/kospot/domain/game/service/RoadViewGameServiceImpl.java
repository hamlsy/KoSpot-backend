package com.kospot.kospot.domain.game.service;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.kospot.domain.game.adaptor.RoadViewGameAdaptor;
import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.dto.response.StartGameResponse;
import com.kospot.kospot.domain.game.entity.GameMode;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.repository.RoadViewGameRepository;
import com.kospot.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
//todo add point system
public class RoadViewGameServiceImpl implements RoadViewGameService {

    private final MemberAdaptor memberAdaptor;
    private final PointService pointService;

    private final AESService aesService;
    private final CoordinateService coordinateService;
    private final RoadViewGameAdaptor adaptor;
    private final RoadViewGameRepository repository;

    @Override
    public StartGameResponse.RoadView startPracticeGame(String sidoKey){
        Coordinate coordinate = coordinateService.getRandomCoordinateBySido(sidoKey);
        RoadViewGame game = RoadViewGame.create(coordinate, null, GameMode.PRACTICE); //todo add member
        repository.save(game);
        //todo refactor
        return StartGameResponse.RoadView.builder()
                .gameId(toEncryptString(game.getId()))
                .targetLat(toEncryptString(game.getTargetLat()))
                .targetLng(toEncryptString(game.getTargetLng()))
                .build();
    }

    @Override
    public EndGameResponse.RoadViewPractice endPracticeGame(Long memberId, EndGameRequest.RoadView request){ //todo add member
        RoadViewGame game = adaptor.queryById(request.getGameId());
        endGame(game, request);
        return EndGameResponse.RoadViewPractice.from(game);
    }


    @Override
    public StartGameResponse.RoadView startRankGame() {
        Coordinate coordinate = coordinateService.getRandomNationwideCoordinate();
        RoadViewGame game = RoadViewGame.create(coordinate, null, GameMode.RANK); //todo add member
        repository.save(game);
        return StartGameResponse.RoadView.builder()
                .gameId(toEncryptString(game.getId()))
                .targetLat(toEncryptString(game.getTargetLat()))
                .targetLng(toEncryptString(game.getTargetLng()))
                .build();
    }

    @Override
    public EndGameResponse.RoadViewRank endRankGame(EndGameRequest.RoadView request){
        RoadViewGame game = adaptor.queryById(request.getGameId());
        endGame(game, request);
        return EndGameResponse.RoadViewRank.from(game);
    }

    private void endGame(RoadViewGame game, EndGameRequest.RoadView request) {
        game.end(
                request.getSubmittedLat(), request.getSubmittedLng(), request.getAnswerTime(), request.getAnswerDistance()
        );
    }

    private <T> String toEncryptString(T object){
        return aesService.encrypt(String.valueOf(object));
    }
}
