package com.kospot.domain.statistic.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.statistic.entity.MemberStatistic;
import com.kospot.domain.statistic.vo.GameTypeStatistic;
import com.kospot.domain.statistic.vo.MultiGameStatistic;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "game_mode_statistic",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_game_mode_statistic", columnNames = {"member_statistic_id", "game_mode"})
        },
        indexes = {
                @Index(name = "idx_game_mode", columnList = "game_mode"),
                @Index(name = "idx_game_mode_created", columnList = "game_mode, created_date"),
                @Index(name = "idx_member_statistic_id", columnList = "member_statistic_id"),
                @Index(name = "idx_practice_avg_score", columnList = "practice_avg_score DESC"),
                @Index(name = "idx_rank_avg_score", columnList = "rank_avg_score DESC"),
                @Index(name = "idx_multi_avg_score", columnList = "multi_avg_score DESC")
        }
)
public class GameModeStatistic extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_mode_statistic_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_statistic_id", nullable = false)
    private MemberStatistic memberStatistic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameMode gameMode;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "games", column = @Column(name = "practice_games")),
            @AttributeOverride(name = "avgScore", column = @Column(name = "practice_avg_score")),
            @AttributeOverride(name = "totalScore", column = @Column(name = "practice_total_score"))
    })
    private GameTypeStatistic practice;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "games", column = @Column(name = "rank_games")),
            @AttributeOverride(name = "avgScore", column = @Column(name = "rank_avg_score")),
            @AttributeOverride(name = "totalScore", column = @Column(name = "rank_total_score"))
    })
    private GameTypeStatistic rank;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "games", column = @Column(name = "multi_games")),
            @AttributeOverride(name = "avgScore", column = @Column(name = "multi_avg_score")),
            @AttributeOverride(name = "totalScore", column = @Column(name = "multi_total_score")),
            @AttributeOverride(name = "firstPlace", column = @Column(name = "multi_first_place")),
            @AttributeOverride(name = "secondPlace", column = @Column(name = "multi_second_place")),
            @AttributeOverride(name = "thirdPlace", column = @Column(name = "multi_third_place"))
    })
    private MultiGameStatistic multi;

    @Version
    private Long version;

    public static GameModeStatistic create(MemberStatistic memberStatistic, GameMode gameMode) {
        GameModeStatistic statistic = new GameModeStatistic();
        statistic.memberStatistic = memberStatistic;
        statistic.gameMode = gameMode;
        statistic.practice = GameTypeStatistic.initialize();
        statistic.rank = GameTypeStatistic.initialize();
        statistic.multi = MultiGameStatistic.initialize();
        return statistic;
    }

    public void updatePractice(double score) {
        practice.update(score);
    }

    public void updateRank(double score) {
        rank.update(score);
    }

    public void updateMulti(double score, Integer rank) {
        multi.update(score, rank);
    }

    public long getTotalGames() {
        return practice.getGames() + rank.getGames() + multi.getGames();
    }

    public double getOverallAvgScore() {
        double totalScore = practice.getTotalScore() + rank.getTotalScore() + multi.getTotalScore();
        long totalGames = getTotalGames();
        return totalGames > 0 ? totalScore / totalGames : 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameModeStatistic that = (GameModeStatistic) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }



}

