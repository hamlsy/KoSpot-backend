package com.kospot.domain.game.service;

import com.kospot.domain.coordinate.adaptor.CoordinateAdaptor;
import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.entity.Sido;
import com.kospot.domain.game.util.DistanceCalculator;
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

    private final CoordinateAdaptor coordinateAdaptor;
    private final RoadViewGameRepository repository;

    private static final int RECOVERY_SCORE = 100;

    public RoadViewGame startPracticeGame(Member member, String sidoKey) {
        Sido sido = Sido.fromKey(sidoKey);
        Coordinate coordinate = coordinateAdaptor.getRandomCoordinateBySido(sido);
        RoadViewGame game = RoadViewGame.create(coordinate, member, GameType.PRACTICE, sido);
        repository.save(game);

        return game;
    }

    public RoadViewGame updateCoordinate(RoadViewGame game, Coordinate coordinate) {
        game.setCoordinate(coordinate);
        return game;
    }

    public RoadViewGame finishGame(Member member, RoadViewGame game, EndGameRequest.RoadView request) {
        endGameUpdate(member, game, request);

        return game;
    }

    public RoadViewGame startRankGame(Member member) {
        Coordinate coordinate = coordinateAdaptor.getRandomCoordinate();
        RoadViewGame game = RoadViewGame.create(coordinate, member, GameType.RANK, null);
        repository.save(game);

        return game;
    }


    private void endGameUpdate(Member member, RoadViewGame game,
                               EndGameRequest.RoadView request) {
        double distance = DistanceCalculator.calculateHaversineDistance(
                request.getSubmittedLat(), request.getSubmittedLng(),
                game.getCoordinate()
        );
        game.end(
                member, request.getSubmittedLat(), request.getSubmittedLng(),
                request.getAnswerTime(), distance
        );
    }

}
