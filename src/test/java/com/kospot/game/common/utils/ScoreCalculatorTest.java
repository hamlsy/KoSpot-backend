package com.kospot.game.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreCalculatorTest {

    @Test
    @DisplayName("거리 기준점 점수를 정확히 반환한다")
    void calculateBaseScoreAtBreakpoints() {
        assertEquals(1000.0, ScoreCalculator.calculateBaseScore(0.0));
        assertEquals(950.0, ScoreCalculator.calculateBaseScore(1.0));
        assertEquals(900.0, ScoreCalculator.calculateBaseScore(5.0));
        assertEquals(800.0, ScoreCalculator.calculateBaseScore(10.0));
        assertEquals(700.0, ScoreCalculator.calculateBaseScore(30.0));
        assertEquals(600.0, ScoreCalculator.calculateBaseScore(50.0));
        assertEquals(400.0, ScoreCalculator.calculateBaseScore(100.0));
        assertEquals(200.0, ScoreCalculator.calculateBaseScore(200.0));
        assertEquals(0.0, ScoreCalculator.calculateBaseScore(400.0));
    }

    @Test
    @DisplayName("유예시간 내에는 최대 배율 1.3을 적용한다")
    void calculateTimeMultiplierInGracePeriod() {
        double multiplier = ScoreCalculator.calculateTimeMultiplier(4_000L, 180_000L, 5_000L);
        assertEquals(1.3, multiplier);
    }

    @Test
    @DisplayName("제한시간 종료 시점 배율은 1.0이다")
    void calculateTimeMultiplierAtLimit() {
        double multiplier = ScoreCalculator.calculateTimeMultiplier(180_000L, 180_000L, 5_000L);
        assertEquals(1.0, multiplier);
    }

    @Test
    @DisplayName("최종 점수는 기본 점수 대비 감소하지 않는다")
    void finalScoreNeverLessThanBaseScore() {
        double base = ScoreCalculator.calculateBaseScore(10.0);
        double finalScore = ScoreCalculator.calculateFinalScore(10.0, 170_000L, 180_000L, 5_000L);

        assertTrue(finalScore >= base);
        assertTrue(finalScore <= base * 1.3);
    }
}
