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

    // 로드뷰 모드 통계
    private long roadviewPracticeGames;
    private double roadviewPracticeAvgScore;
    private double roadviewPracticeTotalScore;

    private long roadviewRankGames;
    private double roadviewRankAvgScore;
    private double roadviewRankTotalScore;

    private long roadviewMultiGames;
    private double roadviewMultiAvgScore;
    private double roadviewMultiTotalScore;

    private long roadviewMultiFirstPlace;
    private long roadviewMultiSecondPlace;
    private long roadviewMultiThirdPlace;

    // 포토 모드 통계
    private long photoPracticeGames;
    private double photoPracticeAvgScore;
    private double photoPracticeTotalScore;

    private long photoRankGames;
    private double photoRankAvgScore;
    private double photoRankTotalScore;

    private long photoMultiGames;
    private double photoMultiAvgScore;
    private double photoMultiTotalScore;

    private long photoMultiFirstPlace;
    private long photoMultiSecondPlace;
    private long photoMultiThirdPlace;

    private double bestScore;

    private int currentStreak;
    private int longestStreak;
    private LocalDate lastPlayedDate;
    private LocalDateTime lastPlayedAt;

    public static MemberStatistic create(Member member) {
        return MemberStatistic.builder()
                .member(member)
                .roadviewPracticeGames(0L)
                .roadviewPracticeAvgScore(0.0)
                .roadviewPracticeTotalScore(0.0)
                .roadviewRankGames(0L)
                .roadviewRankAvgScore(0.0)
                .roadviewRankTotalScore(0.0)
                .roadviewMultiGames(0L)
                .roadviewMultiAvgScore(0.0)
                .roadviewMultiTotalScore(0.0)
                .roadviewMultiFirstPlace(0L)
                .roadviewMultiSecondPlace(0L)
                .roadviewMultiThirdPlace(0L)
                .photoPracticeGames(0L)
                .photoPracticeAvgScore(0.0)
                .photoPracticeTotalScore(0.0)
                .photoRankGames(0L)
                .photoRankAvgScore(0.0)
                .photoRankTotalScore(0.0)
                .photoMultiGames(0L)
                .photoMultiAvgScore(0.0)
                .photoMultiTotalScore(0.0)
                .photoMultiFirstPlace(0L)
                .photoMultiSecondPlace(0L)
                .photoMultiThirdPlace(0L)
                .bestScore(0.0)
                .currentStreak(0)
                .longestStreak(0)
                .build();
    }

    // 로드뷰 모드 업데이트 메서드
    public void updateRoadviewPracticeGame(double score, LocalDate playDate, LocalDateTime playTime) {
        this.roadviewPracticeGames++;
        this.roadviewPracticeTotalScore += score;
        this.roadviewPracticeAvgScore = this.roadviewPracticeTotalScore / this.roadviewPracticeGames;
        updateBestScore(score);
        updateStreak(playDate);
        updateLastPlayed(playTime);
    }

    public void updateRoadviewRankGame(double score, LocalDate playDate, LocalDateTime playTime) {
        this.roadviewRankGames++;
        this.roadviewRankTotalScore += score;
        this.roadviewRankAvgScore = this.roadviewRankTotalScore / this.roadviewRankGames;
        updateBestScore(score);
        updateStreak(playDate);
        updateLastPlayed(playTime);
    }

    public void updateRoadviewMultiGame(double score, Integer rank, LocalDate playDate, LocalDateTime playTime) {
        this.roadviewMultiGames++;
        this.roadviewMultiTotalScore += score;
        this.roadviewMultiAvgScore = this.roadviewMultiTotalScore / this.roadviewMultiGames;
        
        if (rank != null) {
            if (rank == 1) this.roadviewMultiFirstPlace++;
            else if (rank == 2) this.roadviewMultiSecondPlace++;
            else if (rank == 3) this.roadviewMultiThirdPlace++;
        }
        
        updateStreak(playDate);
        updateLastPlayed(playTime);
    }

    // 포토 모드 업데이트 메서드
    public void updatePhotoPracticeGame(double score, LocalDate playDate, LocalDateTime playTime) {
        this.photoPracticeGames++;
        this.photoPracticeTotalScore += score;
        this.photoPracticeAvgScore = this.photoPracticeTotalScore / this.photoPracticeGames;
        updateBestScore(score);
        updateStreak(playDate);
        updateLastPlayed(playTime);
    }

    public void updatePhotoRankGame(double score, LocalDate playDate, LocalDateTime playTime) {
        this.photoRankGames++;
        this.photoRankTotalScore += score;
        this.photoRankAvgScore = this.photoRankTotalScore / this.photoRankGames;
        updateBestScore(score);
        updateStreak(playDate);
        updateLastPlayed(playTime);
    }

    public void updatePhotoMultiGame(double score, Integer rank, LocalDate playDate, LocalDateTime playTime) {
        this.photoMultiGames++;
        this.photoMultiTotalScore += score;
        this.photoMultiAvgScore = this.photoMultiTotalScore / this.photoMultiGames;
        
        if (rank != null) {
            if (rank == 1) this.photoMultiFirstPlace++;
            else if (rank == 2) this.photoMultiSecondPlace++;
            else if (rank == 3) this.photoMultiThirdPlace++;
        }
        
        updateStreak(playDate);
        updateLastPlayed(playTime);
    }

    // 하위 호환성을 위한 Deprecated 메서드 (기존 코드가 있을 수 있으므로)
    @Deprecated
    public void updateSinglePracticeGame(double score, LocalDate playDate, LocalDateTime playTime) {
        updateRoadviewPracticeGame(score, playDate, playTime);
    }

    @Deprecated
    public void updateSingleRankGame(double score, LocalDate playDate, LocalDateTime playTime) {
        updateRoadviewRankGame(score, playDate, playTime);
    }

    @Deprecated
    public void updateMultiGame(double score, Integer rank, LocalDate playDate, LocalDateTime playTime) {
        updateRoadviewMultiGame(score, rank, playDate, playTime);
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

