package com.kospot.game.domain.entity;

import com.kospot.coordinate.domain.entity.Coordinate;
import com.kospot.coordinate.domain.entity.Sido;
import com.kospot.game.common.utils.ScoreCalculator;
import com.kospot.game.domain.vo.GameMode;
import com.kospot.game.domain.vo.GameStatus;
import com.kospot.game.domain.vo.GameType;
import com.kospot.gamerank.common.utils.RatingScoreCalculator;
import com.kospot.member.domain.entity.Member;
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

    private static final long SINGLE_RANK_LIMIT_MS = 180_000L;

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
        this.score = calculateGameScore(answerDistance, answerTime);
    }

    private double calculateGameScore(double distance, double answerTime) {
        if (GameType.RANK.equals(getGameType())) {
            long elapsedMs = normalizeRankElapsedMs(answerTime);
            return ScoreCalculator.calculateFinalScore(
                    distance,
                    elapsedMs,
                    SINGLE_RANK_LIMIT_MS,
                    ScoreCalculator.DEFAULT_GRACE_PERIOD_MS
            );
        }
        return ScoreCalculator.calculateBaseScore(distance);
    }

    private int getChangeRatingScore(int currentRatingScore) {
        return RatingScoreCalculator.calculateRatingChange(this.score, currentRatingScore);
    }

    private long normalizeRankElapsedMs(double answerTime) {
        long raw = Math.round(answerTime);
        return Math.max(0L, Math.min(raw, SINGLE_RANK_LIMIT_MS));
    }

    public double getBaseScore() {
        return ScoreCalculator.calculateBaseScore(this.answerDistance);
    }

    public double getBonusScore() {
        double bonus = this.score - getBaseScore();
        return Math.max(0.0, bonus);
    }
}
