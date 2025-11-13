package com.kospot.domain.statistic.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.game.vo.GameType;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.statistic.vo.PlayStreak;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@SuperBuilder
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

    @Embedded
    private PlayStreak playStreak;

    private LocalDateTime lastPlayedAt;

    public static MemberStatistic create(Member member) {
        MemberStatistic statistic = new MemberStatistic();
        statistic.member = member;
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
        
        updateCommonStatistics(playDate, playTime);
    }

    public void updateMultiGameStatistic(GameMode gameMode, double score, Integer rank,
                                         LocalDate playDate, LocalDateTime playTime) {
        GameModeStatistic modeStatistic = findModeStatistic(gameMode);
        modeStatistic.updateMulti(score, rank);
        updateCommonStatistics(playDate, playTime);
    }

    public GameModeStatistic findModeStatistic(GameMode gameMode) {
        return modeStatistics.stream()
                .filter(stat -> stat.getGameMode() == gameMode)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("GameModeStatistic not found: " + gameMode));
    }

    private void updateCommonStatistics(LocalDate playDate, LocalDateTime playTime) {
        playStreak.update(playDate);
        updateLastPlayed(playTime);
    }

    private void updateLastPlayed(LocalDateTime playTime) {
        if (this.lastPlayedAt == null || playTime.isAfter(this.lastPlayedAt)) {
            this.lastPlayedAt = playTime;
        }
    }

}

