package com.kospot.domain.game.entity;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.game.util.ScoreCalculator;
import com.kospot.domain.gameRank.util.RatingScoreCalculator;
import com.kospot.domain.member.entity.Member;
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

    private double score;

    public static RoadViewGame create(Coordinate coordinate, Member member, GameType gameType) {
        return RoadViewGame.builder()
                .targetLat(coordinate.getLat())
                .targetLng(coordinate.getLng())
                .member(member)
                .gameMode(GameMode.ROADVIEW)
                .gameType(gameType)
                .poiName(coordinate.getPoiName())
                .gameStatus(GameStatus.ABANDONED)
                .build();
    }

    public void end(Member member, double submittedLat, double submittedLng, double answerTime, double answerDistance) {
        super.end(member, submittedLat, submittedLng, answerTime);
        this.answerDistance = answerDistance;
        this.score = getScore(answerDistance);
    }

    private double getScore(double distance) {
        return ScoreCalculator.calculateScore(distance);
    }

    private int getChangeRatingScore(int currentRatingScore) {
        return RatingScoreCalculator.calculateRatingChange(this.score, currentRatingScore);
    }
}
