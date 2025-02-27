package com.kospot.kospot.domain.game.service;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.kospot.domain.game.adaptor.RoadViewGameAdaptor;
import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.entity.GameMode;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.repository.RoadViewGameRepository;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.entity.PointHistoryType;
import com.kospot.kospot.domain.point.service.PointHistoryService;
import com.kospot.kospot.domain.point.service.PointService;
import com.kospot.kospot.domain.point.util.PointCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RoadViewGameService {

    private final PointService pointService;
    private final PointHistoryService pointHistoryService;

    private final CoordinateService coordinateService;
    private final RoadViewGameAdaptor adaptor;
    private final RoadViewGameRepository repository;

    public RoadViewGame startPracticeGame(Member member, String sidoKey){
        Coordinate coordinate = coordinateService.getRandomCoordinateBySido(sidoKey);
        RoadViewGame game = RoadViewGame.create(coordinate, member, GameMode.PRACTICE);
        repository.save(game);

        return game;
    }

    //todo refactor transaction
    public RoadViewGame endPracticeGame(Member member, EndGameRequest.RoadView request){
        //end game
        RoadViewGame game = adaptor.queryById(request.getGameId());
        endGame(member, game, request);

        return game;
    }

    public RoadViewGame startRankGame(Member member) {
        Coordinate coordinate = coordinateService.getRandomNationwideCoordinate();
        RoadViewGame game = RoadViewGame.create(coordinate, member, GameMode.RANK);
        repository.save(game);

        return game;
    }

    //todo refactor transaction
    public RoadViewGame endRankGame(Member member, EndGameRequest.RoadView request){
        RoadViewGame game = adaptor.queryById(request.getGameId());
        endGame(member, game, request);

        return game;
    }

    private void endGame(Member member, RoadViewGame game, EndGameRequest.RoadView request) {
        game.end(
                member, request.getSubmittedLat(), request.getSubmittedLng(), request.getAnswerTime(), request.getAnswerDistance()
        );
    }


}
