package com.kospot.domain.game.entity;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.entity.Sido;
import com.kospot.domain.game.util.ScoreCalculator;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.game.vo.GameStatus;
import com.kospot.domain.game.vo.GameType;
import com.kospot.domain.gamerank.util.RatingScoreCalculator;
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
@Table(
        name = "road_view_game",
        indexes = {
                @Index(name = "idx_road_view_game_mvp", columnList = "game_mode, game_status, ended_at, score, id"),
                @Index(name = "idx_road_view_game_member_status_created", columnList = "member_member_id, game_status, created_date")
        }
)
public class RoadViewGame extends Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double answerDistance;

    private String poiName;

    private double score;

    @Enumerated(EnumType.STRING)
    private Sido practiceSido;

    @Override
    public Long getId() {
        return id;
    }

    public static RoadViewGame create(Coordinate coordinate, Member member, GameType gameType, Sido practiceSido) {
        return RoadViewGame.builder()
                .coordinate(coordinate)
                .member(member)
                .gameMode(GameMode.ROADVIEW)
                .gameType(gameType)
                .poiName(coordinate.getPoiName())
                .gameStatus(GameStatus.ABANDONED)
                .practiceSido(practiceSido)
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
