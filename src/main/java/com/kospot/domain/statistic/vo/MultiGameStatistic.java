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
public class MultiGameStatistic {
    private long games;
    private double avgScore;
    private double totalScore;
    private long firstPlace;
    private long secondPlace;
    private long thirdPlace;

    public static MultiGameStatistic initialize() {
        return new MultiGameStatistic(0L, 0.0, 0.0, 0L, 0L, 0L);
    }

    public void update(double score, Integer rank) {
        this.games++;
        this.totalScore += score;
        this.avgScore = this.totalScore / this.games;

        if (rank != null) {
            switch (rank) {
                case 1 -> this.firstPlace++;
                case 2 -> this.secondPlace++;
                case 3 -> this.thirdPlace++;
            }
        }
    }
    public double getWinRate() {
        return games > 0 ? (double) firstPlace / games * 100 : 0.0;
    }
}