package com.kospot.game.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.TreeMap;

public class ScoreCalculator {

    public static final double ZERO_SCORE = 0.0;
    public static final double MAX_SCORE = 1000.0;
    public static final double MAX_TIME_MULTIPLIER = 1.3;
    public static final double MIN_TIME_MULTIPLIER = 1.0;
    public static final long DEFAULT_GRACE_PERIOD_MS = 5_000L;

    private static final double D_MAX = 400.0; // 400km 이상 → 0점

    private static final TreeMap<Double, Double> DISTANCE_SCORE_TABLE = new TreeMap<>();

    private static final BigDecimal ZERO_SCORE_BD = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE_BD = BigDecimal.valueOf(MAX_SCORE);

    private static final double MULTI_GAME_SCALE = 0.08; // 멀티게임 환산 배수

    static {
        DISTANCE_SCORE_TABLE.put(0.0, 1000.0);
        DISTANCE_SCORE_TABLE.put(1.0, 950.0);
        DISTANCE_SCORE_TABLE.put(5.0, 900.0);
        DISTANCE_SCORE_TABLE.put(10.0, 800.0);
        DISTANCE_SCORE_TABLE.put(30.0, 700.0);
        DISTANCE_SCORE_TABLE.put(50.0, 600.0);
        DISTANCE_SCORE_TABLE.put(100.0, 400.0);
        DISTANCE_SCORE_TABLE.put(200.0, 200.0);
        DISTANCE_SCORE_TABLE.put(400.0, 0.0);
    }

    /**
     * 거리 기반 점수 계산 (구간별 선형 보간)
     */
    public static double calculateBaseScore(double distance) {
        double normalizedDistance = Math.max(0.0, distance);
        if (normalizedDistance >= D_MAX) {
            return ZERO_SCORE;
        }

        Map.Entry<Double, Double> lower = DISTANCE_SCORE_TABLE.floorEntry(normalizedDistance);
        Map.Entry<Double, Double> upper = DISTANCE_SCORE_TABLE.ceilingEntry(normalizedDistance);

        if (lower == null) {
            return MAX_SCORE;
        }
        if (upper == null) {
            return ZERO_SCORE;
        }
        if (lower.getKey().equals(upper.getKey())) {
            return normalizeScore(lower.getValue());
        }

        double interpolated = lerp(
                normalizedDistance,
                lower.getKey(),
                upper.getKey(),
                lower.getValue(),
                upper.getValue()
        );
        return normalizeScore(interpolated);
    }

    public static double calculateTimeMultiplier(long elapsedMs, long limitMs, long graceMs) {
        if (limitMs <= 0) {
            return MIN_TIME_MULTIPLIER;
        }

        long normalizedElapsed = Math.max(0L, Math.min(elapsedMs, limitMs));
        long normalizedGrace = Math.max(0L, Math.min(graceMs, limitMs));

        if (normalizedElapsed <= normalizedGrace) {
            return MAX_TIME_MULTIPLIER;
        }

        long decayWindow = limitMs - normalizedGrace;
        if (decayWindow <= 0) {
            return MIN_TIME_MULTIPLIER;
        }

        double ratio = (double) (limitMs - normalizedElapsed) / decayWindow;
        double multiplier = MIN_TIME_MULTIPLIER + (MAX_TIME_MULTIPLIER - MIN_TIME_MULTIPLIER) * ratio;
        return clamp(multiplier, MIN_TIME_MULTIPLIER, MAX_TIME_MULTIPLIER);
    }

    public static double calculateFinalScore(double distance, long elapsedMs, long limitMs, long graceMs) {
        double baseScore = calculateBaseScore(distance);
        if (baseScore <= ZERO_SCORE) {
            return ZERO_SCORE;
        }
        double multiplier = calculateTimeMultiplier(elapsedMs, limitMs, graceMs);
        return normalizeNonNegative(baseScore * multiplier);
    }

    public static double calculateFinalMultiScore(double distance, long elapsedMs, long limitMs, long graceMs) {
        return normalizeNonNegative(calculateFinalScore(distance, elapsedMs, limitMs, graceMs) * MULTI_GAME_SCALE);
    }

    /**
     * 멀티 게임 점수 환산 (싱글 대비 8%)
     */
    @Deprecated
    public static double calculateMultiGameScore(double distance) {
        return calculateFinalMultiScore(distance, 0L, 1L, 0L);
    }

    /**
     * 거리 기반 점수 계산 (구간별 선형 보간)
     */
    @Deprecated
    public static double calculateScore(double distance) {
        return calculateBaseScore(distance);
    }

    /**
     * 선형 보간: x가 [x1, x2]에 있을 때 y를 [y1, y2]로 선형 변환
     */
    private static double lerp(double x, double x1, double x2, double y1, double y2) {
        if (x2 - x1 == 0) {
            return y1;
        }
        double t = (x - x1) / (x2 - x1);
        return y1 + t * (y2 - y1);
    }

    private static double normalizeScore(double value) {
        BigDecimal rounded = BigDecimal
                .valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);

        if (rounded.compareTo(ZERO_SCORE_BD) < 0) {
            return ZERO_SCORE;
        }
        if (rounded.compareTo(MAX_SCORE_BD) > 0) {
            return MAX_SCORE_BD.doubleValue();
        }

        return rounded.doubleValue();
    }

    private static double normalizeNonNegative(double value) {
        BigDecimal rounded = BigDecimal
                .valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);

        if (rounded.compareTo(ZERO_SCORE_BD) < 0) {
            return ZERO_SCORE;
        }

        return rounded.doubleValue();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

}
