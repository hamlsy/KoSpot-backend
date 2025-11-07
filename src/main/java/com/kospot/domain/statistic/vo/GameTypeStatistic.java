package com.kospot.domain.statistic.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GameTypeStatistic {
    private long games;
    private double avgScore;
    private double totalScore;

    public static GameTypeStatistic initialize() {
        return new GameTypeStatistic(0L, 0.0, 0.0);
    }

    public void update(double score) {
        this.games++;
        this.totalScore += score;
        this.avgScore = this.totalScore / this.games;
    }
}
