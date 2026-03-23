package com.kospot.game.application.service;

import com.kospot.coordinate.application.adaptor.CoordinateAdaptor;
import com.kospot.coordinate.domain.entity.Coordinate;
import com.kospot.coordinate.domain.entity.Sido;
import com.kospot.game.common.utils.DistanceCalculator;
import com.kospot.game.presentation.dto.request.EndGameRequest;
import com.kospot.game.domain.vo.GameType;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.infrastructure.persistence.RoadViewGameRepository;
import com.kospot.member.domain.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

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


    public RoadViewGame startAnonymousPracticeGame(String sidoKey) {
        Sido sido = Sido.fromKey(sidoKey);
        Coordinate coordinate = coordinateAdaptor.getRandomCoordinateBySido(sido);
        RoadViewGame game = RoadViewGame.create(coordinate, null, GameType.PRACTICE, sido);
        repository.save(game);
        return game;
    }

    public RoadViewGame finishGameAnonymous(RoadViewGame game, EndGameRequest.RoadView request) {
        double distance = DistanceCalculator.calculateHaversineDistance(
                request.getSubmittedLat(), request.getSubmittedLng(),
                game.getCoordinate()
        );
        double normalizedAnswerTime = getNormalizedAnswerTime(request.getAnswerTime());
        game.endAnonymous(
                request.getSubmittedLat(), request.getSubmittedLng(),
                normalizedAnswerTime, distance
        );
        return game;
    }

    private void endGameUpdate(Member member, RoadViewGame game,
                               EndGameRequest.RoadView request) {
        double distance = DistanceCalculator.calculateHaversineDistance(
                request.getSubmittedLat(), request.getSubmittedLng(),
                game.getCoordinate()
        );
        double normalizedAnswerTime = getNormalizedAnswerTime(request.getAnswerTime());
        game.end(
                member, request.getSubmittedLat(), request.getSubmittedLng(),
                normalizedAnswerTime, distance
        );
    }

    private double getNormalizedAnswerTime(double answerTime) {
        return BigDecimal.valueOf(answerTime)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }

}
