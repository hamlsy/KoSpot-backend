package com.kospot.domain.game.service;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.domain.game.adaptor.RoadViewGameAdaptor;
import com.kospot.presentation.game.dto.request.EndGameRequest;
import com.kospot.domain.game.vo.GameType;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.repository.RoadViewGameRepository;
import com.kospot.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RoadViewGameService {

    private final CoordinateService coordinateService;
    private final RoadViewGameAdaptor adaptor;
    private final RoadViewGameRepository repository;

    private static final int RECOVERY_SCORE = 100;

    public RoadViewGame startPracticeGame(Member member, String sidoKey) {
        Coordinate coordinate = coordinateService.getRandomCoordinateBySido(sidoKey);
        RoadViewGame game = RoadViewGame.create(coordinate, member, GameType.PRACTICE);
        repository.save(game);

        return game;
    }

    public RoadViewGame endGame(Member member, EndGameRequest.RoadView request) {
        RoadViewGame game = adaptor.queryById(request.getGameId());
        endGameUpdate(member, game, request);

        return game;
    }

    public RoadViewGame startRankGame(Member member) {
        Coordinate coordinate = coordinateService.getRandomNationwideCoordinate();
        RoadViewGame game = RoadViewGame.create(coordinate, member, GameType.RANK);
        repository.save(game);

        return game;
    }


    private void endGameUpdate(Member member, RoadViewGame game, EndGameRequest.RoadView request) {
        game.end(
                member, request.getSubmittedLat(), request.getSubmittedLng(),
                request.getAnswerTime(), request.getAnswerDistance()
        );
    }

}
