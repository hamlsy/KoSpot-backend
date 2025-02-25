package com.kospot.kospot.domain.game.entity;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.game.util.ScoreCalculator;
import com.kospot.kospot.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadViewGame extends Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double answerDistance;

    private String poiName;

    public static RoadViewGame create(Coordinate coordinate, Member member, GameMode gameMode) {
        return RoadViewGame.builder()
                .targetLat(coordinate.getLat())
                .targetLng(coordinate.getLng())
                .member(member)
                .gameType(GameType.ROADVIEW)
                .gameMode(gameMode)
                .poiName(coordinate.getPoiName())
                .gameStatus(GameStatus.ABANDONED)
                .build();
    }

    public void end(Member member, double submittedLat, double submittedLng, double answerTime, double answerDistance) {
        super.end(member, submittedLat, submittedLng, getScore(answerDistance), answerTime);
        this.answerDistance = answerDistance;
    }

    private double getScore(double distance) {
        return ScoreCalculator.calculateScore(distance);
    }
}
