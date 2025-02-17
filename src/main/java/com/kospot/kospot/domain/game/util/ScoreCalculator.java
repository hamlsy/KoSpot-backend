package com.kospot.kospot.domain.game.util;

public class ScoreCalculator {
    public static final double MAX_SCORE = 1000.0;
    private static final double D_MAX = 400.0; // 400km 이상이면 0점
    private static final double D_S = 200.0; // 감쇠 거리 (작게 설정하여 점수 차이 강화)

    public static double calculateScore(double distance) {
        if (distance >= D_MAX) {
            return 0.0;
        }

        // 기본 점수 (지수 감소) -> 점수 차이를 강화
        double baseScore = MAX_SCORE * Math.exp(-distance / D_S);

        // 최종 점수 계산 (0점 미만 방지 및 최대 점수 제한)
        return Math.max(0, Math.min(MAX_SCORE, Math.round(baseScore * 100) / 100.0));
    }

}
