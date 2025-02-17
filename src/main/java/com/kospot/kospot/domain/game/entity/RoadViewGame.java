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

    private double answerDistance;

    private String poiName;

    public static RoadViewGame create(Coordinate coordinate, Member member, GameType gameType){
        return RoadViewGame.builder()
                .targetLat(coordinate.getLat())
                .targetLat(coordinate.getLng())
                .member(member)
                .gameType(gameType)
                .gameStatus(GameStatus.ABANDONED)
                .build();
    }

    public void end(double submittedLat, double submittedLng, double answerDistance){
        super.end(submittedLat, submittedLng, getScore(answerDistance)); //todo add score
        this.answerDistance = answerDistance;
    }

    private double getScore(double distance){
        return ScoreCalculator.calculateScore(distance);
    }
}
