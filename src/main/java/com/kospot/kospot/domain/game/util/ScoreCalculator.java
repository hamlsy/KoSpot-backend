package com.kospot.kospot.domain.game.util;

public class ScoreCalculator {
    public static final double MAX_SCORE = 1000.0;
    private static final double D_MAX = 400.0; // 400km 이상이면 0점
    private static final double D_S = 165.0; // 감쇠 거리 (튜닝 필요)
    private static final double D_BONUS = 5.0; // 보너스 거리
    private static final double BONUS_MAX = 200.0; // 최대 보너스 점수
    private static final double BONUS_SLOPE = 1.0; // 보너스 증가율

    public static double calculateScore(double distance) {
        if (distance >= D_MAX) {
            return 0.0;
        }

        // 기본 점수 (지수 감소)
        double baseScore = MAX_SCORE * Math.exp(-distance / D_S);

        // 근거리 보너스 점수 (시그모이드 함수)
        double bonus = BONUS_MAX / (1 + Math.exp((distance - D_BONUS) / BONUS_SLOPE));

        // 최종 점수 (0점 미만 방지)
        return Math.max(0, Math.round((baseScore + bonus) * 100) / 100.0);
    }

}
