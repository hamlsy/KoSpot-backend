package com.kospot.domain.game.util;

public class ScoreCalculator {

    public static final double ZERO_SCORE = 0.0;
    public static final double MAX_SCORE = 1000.0;

    private static final double D_MAX = 300.0;        // 300km 이상이면 0점 처리 (기존 400km)
    private static final double D_S = 60.0;           // 감쇠 거리 - 급격한 하락 유도
    private static final double SHARPNESS = 1.35;     // 지수 감쇠 가중치
    private static final double MULTI_GAME_SCALE = 0.08; // 멀티게임 환산 배수 (1000점 → 80점)

    /**
     * 거리 기반 점수 계산 (더 빡빡한 커브)
     * 예시:
     * - 0.5km → 약 960점
     * - 5km   → 약 710점
     * - 20km  → 약 270점
     * - 100km → 약 20점
     */
    public static double calculateScore(double distance) {
        if (distance <= 0) {
            return MAX_SCORE;
        }
        if (distance >= D_MAX) {
            return ZERO_SCORE;
        }

        double normalized = Math.exp(-Math.pow(distance / D_S, SHARPNESS));
        double baseScore = MAX_SCORE * normalized;
        double rounded = Math.round(baseScore * 100) / 100.0;
        return Math.max(ZERO_SCORE, Math.min(MAX_SCORE, rounded));
    }

    /**
     * 멀티 게임 점수 환산 (싱글 대비 8%)
     * 예: 거리 5km → 싱글 710점 → 멀티 약 56.8점
     */
    public static double calculateMultiGameScore(double distance) {
        double score = calculateScore(distance) * MULTI_GAME_SCALE;
        return Math.round(score * 100) / 100.0;
    }

}
