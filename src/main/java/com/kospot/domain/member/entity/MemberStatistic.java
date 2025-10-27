package com.kospot.domain.member.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_statistic", indexes = {
        @Index(name = "idx_member_statistic_member_id", columnList = "member_id")
})
public class MemberStatistic extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_statistic_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", unique = true)
    private Member member;

    private long singlePracticeGames;
    private double singlePracticeAvgScore;
    private double singlePracticeTotalScore;

    private long singleRankGames;
    private double singleRankAvgScore;
    private double singleRankTotalScore;

    private long multiGames;
    private double multiAvgScore;
    private double multiTotalScore;

    private long multiFirstPlace;
    private long multiSecondPlace;
    private long multiThirdPlace;

    private double bestScore;

    private int currentStreak;
    private int longestStreak;
    private LocalDate lastPlayedDate;
    private LocalDateTime lastPlayedAt;

    public static MemberStatistic create(Member member) {
        return MemberStatistic.builder()
                .member(member)
                .singlePracticeGames(0L)
                .singlePracticeAvgScore(0.0)
                .singlePracticeTotalScore(0.0)
                .singleRankGames(0L)
                .singleRankAvgScore(0.0)
                .singleRankTotalScore(0.0)
                .multiGames(0L)
                .multiAvgScore(0.0)
                .multiTotalScore(0.0)
                .multiFirstPlace(0L)
                .multiSecondPlace(0L)
                .multiThirdPlace(0L)
                .bestScore(0.0)
                .currentStreak(0)
                .longestStreak(0)
                .build();
    }

    public void updateSinglePracticeGame(double score, LocalDate playDate, LocalDateTime playTime) {
        this.singlePracticeGames++;
        this.singlePracticeTotalScore += score;
        this.singlePracticeAvgScore = this.singlePracticeTotalScore / this.singlePracticeGames;
        updateBestScore(score);
        updateStreak(playDate);
        updateLastPlayed(playTime);
    }

    public void updateSingleRankGame(double score, LocalDate playDate, LocalDateTime playTime) {
        this.singleRankGames++;
        this.singleRankTotalScore += score;
        this.singleRankAvgScore = this.singleRankTotalScore / this.singleRankGames;
        updateBestScore(score);
        updateStreak(playDate);
        updateLastPlayed(playTime);
    }

    public void updateMultiGame(double score, Integer rank, LocalDate playDate, LocalDateTime playTime) {
        this.multiGames++;
        this.multiTotalScore += score;
        this.multiAvgScore = this.multiTotalScore / this.multiGames;
        
        if (rank != null) {
            if (rank == 1) this.multiFirstPlace++;
            else if (rank == 2) this.multiSecondPlace++;
            else if (rank == 3) this.multiThirdPlace++;
        }
        
        updateStreak(playDate);
        updateLastPlayed(playTime);
    }

    private void updateBestScore(double score) {
        if (score > this.bestScore) {
            this.bestScore = score;
        }
    }

    private void updateStreak(LocalDate playDate) {
        if (this.lastPlayedDate == null) {
            this.currentStreak = 1;
            this.longestStreak = 1;
        } else if (playDate.equals(this.lastPlayedDate)) {
            return;
        } else if (playDate.equals(this.lastPlayedDate.plusDays(1))) {
            this.currentStreak++;
            if (this.currentStreak > this.longestStreak) {
                this.longestStreak = this.currentStreak;
            }
        } else if (playDate.isAfter(this.lastPlayedDate.plusDays(1))) {
            this.currentStreak = 1;
        }
        this.lastPlayedDate = playDate;
    }

    private void updateLastPlayed(LocalDateTime playTime) {
        if (this.lastPlayedAt == null || playTime.isAfter(this.lastPlayedAt)) {
            this.lastPlayedAt = playTime;
        }
    }
}

