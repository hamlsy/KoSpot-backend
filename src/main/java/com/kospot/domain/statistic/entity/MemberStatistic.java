package com.kospot.domain.statistic.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.game.vo.GameType;
import com.kospot.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
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

    @OneToMany(mappedBy = "memberStatistic", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameModeStatistic> modeStatistics = new ArrayList<>();

    private double bestScore;

    @Embedded
    private PlayStreak playStreak;

    private LocalDateTime lastPlayedAt;

    public static MemberStatistic create(Member member) {
        MemberStatistic statistic = new MemberStatistic();
        statistic.member = member;
        statistic.bestScore = 0.0;
        statistic.playStreak = PlayStreak.initialize();

        for (GameMode mode : GameMode.values()) {
            statistic.modeStatistics.add(GameModeStatistic.create(statistic, mode));
        }

        return statistic;
    }

    public void updateGameStatistic(GameMode gameMode, GameType gameType, double score,
                                    LocalDate playDate, LocalDateTime playTime) {
        GameModeStatistic modeStatistic = findModeStatistic(gameMode);
        
        if (gameType == GameType.PRACTICE) {
            modeStatistic.updatePractice(score);
        } else if (gameType == GameType.RANK) {
            modeStatistic.updateRank(score);
        }
        
        updateCommonStatistics(score, playDate, playTime);
    }

    public void updateMultiGameStatistic(GameMode gameMode, double score, Integer rank,
                                         LocalDate playDate, LocalDateTime playTime) {
        GameModeStatistic modeStatistic = findModeStatistic(gameMode);
        modeStatistic.updateMulti(score, rank);
        updateCommonStatistics(score, playDate, playTime);
    }

    private GameModeStatistic findModeStatistic(GameMode gameMode) {
        return modeStatistics.stream()
                .filter(stat -> stat.getGameMode() == gameMode)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("GameModeStatistic not found: " + gameMode));
    }

    private void updateCommonStatistics(double score, LocalDate playDate, LocalDateTime playTime) {
        updateBestScore(score);
        playStreak.update(playDate);
        updateLastPlayed(playTime);
    }

    private void updateBestScore(double score) {
        if (score > this.bestScore) {
            this.bestScore = score;
        }
    }

    private void updateLastPlayed(LocalDateTime playTime) {
        if (this.lastPlayedAt == null || playTime.isAfter(this.lastPlayedAt)) {
            this.lastPlayedAt = playTime;
        }
    }

    // 하위 호환성을 위한 Deprecated 메서드
    @Deprecated
    public void updateSinglePracticeGame(double score, LocalDate playDate, LocalDateTime playTime) {
        updateGameStatistic(GameMode.ROADVIEW, GameType.PRACTICE, score, playDate, playTime);
    }

    @Deprecated
    public void updateSingleRankGame(double score, LocalDate playDate, LocalDateTime playTime) {
        updateGameStatistic(GameMode.ROADVIEW, GameType.RANK, score, playDate, playTime);
    }

    @Deprecated
    public void updateMultiGame(double score, Integer rank, LocalDate playDate, LocalDateTime playTime) {
        updateMultiGameStatistic(GameMode.ROADVIEW, score, rank, playDate, playTime);
    }

    // 레거시 필드 접근 메서드 (하위 호환성)
    @Deprecated
    public long getRoadviewPracticeGames() {
        return findModeStatistic(GameMode.ROADVIEW).getPractice().getGames();
    }

    @Deprecated
    public double getRoadviewPracticeAvgScore() {
        return findModeStatistic(GameMode.ROADVIEW).getPractice().getAvgScore();
    }

    @Deprecated
    public long getRoadviewRankGames() {
        return findModeStatistic(GameMode.ROADVIEW).getRank().getGames();
    }

    @Deprecated
    public double getRoadviewRankAvgScore() {
        return findModeStatistic(GameMode.ROADVIEW).getRank().getAvgScore();
    }

    @Deprecated
    public long getRoadviewMultiGames() {
        return findModeStatistic(GameMode.ROADVIEW).getMulti().getGames();
    }

    @Deprecated
    public double getRoadviewMultiAvgScore() {
        return findModeStatistic(GameMode.ROADVIEW).getMulti().getAvgScore();
    }

    @Deprecated
    public long getRoadviewMultiFirstPlace() {
        return findModeStatistic(GameMode.ROADVIEW).getMulti().getFirstPlace();
    }

    @Deprecated
    public long getRoadviewMultiSecondPlace() {
        return findModeStatistic(GameMode.ROADVIEW).getMulti().getSecondPlace();
    }

    @Deprecated
    public long getRoadviewMultiThirdPlace() {
        return findModeStatistic(GameMode.ROADVIEW).getMulti().getThirdPlace();
    }

    @Deprecated
    public long getPhotoPracticeGames() {
        return findModeStatistic(GameMode.PHOTO).getPractice().getGames();
    }

    @Deprecated
    public double getPhotoPracticeAvgScore() {
        return findModeStatistic(GameMode.PHOTO).getPractice().getAvgScore();
    }

    @Deprecated
    public long getPhotoRankGames() {
        return findModeStatistic(GameMode.PHOTO).getRank().getGames();
    }

    @Deprecated
    public double getPhotoRankAvgScore() {
        return findModeStatistic(GameMode.PHOTO).getRank().getAvgScore();
    }

    @Deprecated
    public long getPhotoMultiGames() {
        return findModeStatistic(GameMode.PHOTO).getMulti().getGames();
    }

    @Deprecated
    public double getPhotoMultiAvgScore() {
        return findModeStatistic(GameMode.PHOTO).getMulti().getAvgScore();
    }

    @Deprecated
    public long getPhotoMultiFirstPlace() {
        return findModeStatistic(GameMode.PHOTO).getMulti().getFirstPlace();
    }

    @Deprecated
    public long getPhotoMultiSecondPlace() {
        return findModeStatistic(GameMode.PHOTO).getMulti().getSecondPlace();
    }

    @Deprecated
    public long getPhotoMultiThirdPlace() {
        return findModeStatistic(GameMode.PHOTO).getMulti().getThirdPlace();
    }

    @Deprecated
    public int getCurrentStreak() {
        return playStreak.getCurrentStreak();
    }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class PlayStreak {
        private int currentStreak;
        private int longestStreak;
        private LocalDate lastPlayedDate;

        public static PlayStreak initialize() {
            return new PlayStreak(0, 0, null);
        }

        public void update(LocalDate playDate) {
            if (this.lastPlayedDate == null) {
                this.currentStreak = 1;
                this.longestStreak = 1;
            } else if (!playDate.equals(this.lastPlayedDate)) {
                if (playDate.equals(this.lastPlayedDate.plusDays(1))) {
                    this.currentStreak++;
                    if (this.currentStreak > this.longestStreak) {
                        this.longestStreak = this.currentStreak;
                    }
                } else if (playDate.isAfter(this.lastPlayedDate.plusDays(1))) {
                    this.currentStreak = 1;
                }
            }
            this.lastPlayedDate = playDate;
        }
    }
}

