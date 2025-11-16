package com.kospot.domain.game.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ScoreCalculator {

    public static final double ZERO_SCORE = 0.0;
    public static final double MAX_SCORE = 1000.0;

    // 거리 기준점 (km)
    private static final double D0 = 0.0;    // 0km
    private static final double D1 = 1.0;    // 1km → 950점
    private static final double D2 = 5.0;    // 5km → 900점
    private static final double D3 = 10.0;   // 10km → 800점
    private static final double D4 = 30.0;   // 30km → 700점
    private static final double D5 = 50.0;   // 50km → 600점
    private static final double D6 = 100.0;  // 100km → 400점
    private static final double D7 = 200.0;  // 200km → 200점
    private static final double D_MAX = 400.0; // 400km 이상 → 0점

    // 각 거리에서의 목표 점수
    private static final double S0 = MAX_SCORE; // 0km → 1000점
    private static final double S1 = 950.0;     // 1km
    private static final double S2 = 900.0;     // 5km
    private static final double S3 = 800.0;     // 10km
    private static final double S4 = 700.0;     // 30km
    private static final double S5 = 600.0;     // 50km
    private static final double S6 = 400.0;     // 100km
    private static final double S7 = 200.0;     // 200km
    private static final double S8 = 0.0;       // 400km

    private static final BigDecimal ZERO_SCORE_BD = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE_BD = BigDecimal.valueOf(10000.0);

    private static final double MULTI_GAME_SCALE = 0.08; // 멀티게임 환산 배수

    /**
     * 거리 기반 점수 계산 (구간별 선형 보간)
     */
    public static double calculateScore(double distance) {
        if (distance <= 0) {
            return MAX_SCORE;
        }
        if (distance >= D_MAX) {
            return ZERO_SCORE;
        }

        double score;

        if (distance <= D1) {
            // 0 ~ 1km: 1000 → 950
            score = lerp(distance, D0, D1, S0, S1);
        } else if (distance <= D2) {
            // 1 ~ 5km: 950 → 900
            score = lerp(distance, D1, D2, S1, S2);
        } else if (distance <= D3) {
            // 5 ~ 10km: 900 → 800
            score = lerp(distance, D2, D3, S2, S3);
        } else if (distance <= D4) {
            // 10 ~ 30km: 800 → 700
            score = lerp(distance, D3, D4, S3, S4);
        } else if (distance <= D5) {
            // 30 ~ 50km: 700 → 600
            score = lerp(distance, D4, D5, S4, S5);
        } else if (distance <= D6) {
            // 50 ~ 100km: 600 → 400
            score = lerp(distance, D5, D6, S5, S6);
        } else if (distance <= D7) {
            // 100 ~ 200km: 400 → 200
            score = lerp(distance, D6, D7, S6, S7);
        } else {
            // 200 ~ 400km: 200 → 0
            score = lerp(distance, D7, D_MAX, S7, S8);
        }

        // 소수 둘째 자리까지
        BigDecimal rounded = BigDecimal
                .valueOf(score)
                .setScale(2, RoundingMode.HALF_UP);
        if (rounded.compareTo(ZERO_SCORE_BD) < 0) {
            rounded = ZERO_SCORE_BD;
        } else if (rounded.compareTo(MAX_SCORE_BD) > 0) {
            rounded = MAX_SCORE_BD;
        }

        return rounded.doubleValue();
    }

    /**
     * 멀티 게임 점수 환산 (싱글 대비 8%)
     */
    public static double calculateMultiGameScore(double distance) {
        BigDecimal score = BigDecimal
                .valueOf(calculateScore(distance))
                .multiply(BigDecimal.valueOf(MULTI_GAME_SCALE))
                .setScale(2, RoundingMode.HALF_UP);

        return score.doubleValue();
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

}
