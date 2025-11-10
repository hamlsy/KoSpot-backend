package com.kospot.domain.point.util;

import com.kospot.domain.gamerank.vo.RankTier;

public class PointCalculator {

    // 싱글 게임 (로드뷰) 포인트 상수 - 기존 대비 1/10
    private static final double PRACTICE_SCORE_MULTIPLIER = 0.008;  // 연습: 점수당 0.008 포인트
    private static final int PRACTICE_BASE_POINT = 3;               // 연습: 기본 3 포인트

    private static final double RANK_SCORE_MULTIPLIER = 0.02;       // 랭크: 점수당 0.02 포인트
    private static final int RANK_BASE_POINT = 5;                   // 랭크: 기본 5 포인트

    // 멀티 게임 포인트 상수 - 기존 대비 1/10
    private static final double MULTI_SCORE_FACTOR = 0.001;         // 총 점수 기반 배수
    private static final int[] RANK_BONUS_POINTS = {15, 10, 7, 5, 3};
    private static final int DEFAULT_PARTICIPATION_POINT = 2;

    /**
     * 로드뷰 연습 게임 포인트 계산
     * 공식: score * 0.008 + 3
     * 예: 0점 = 3P, 500점 ≒ 7P, 1000점 ≒ 11P
     */
    public static int getPracticePoint(double score){
        return (int) (score * PRACTICE_SCORE_MULTIPLIER + PRACTICE_BASE_POINT);
    }

    /**
     * 로드뷰 랭크 게임 포인트 계산
     * 공식: (score * 0.02 + 5) * tierMultiplier
     * 예: Bronze 1000점 ≒ 25P, Master 1000점 ≒ 62P
     */
    public static int getRankPoint(RankTier tier, double score) {
        double basePoint = (score * RANK_SCORE_MULTIPLIER + RANK_BASE_POINT) * tier.getPointMultiplier();
        return (int) basePoint;
    }

    /**
     * 멀티 게임 포인트 계산 (기존 대비 1/10 축소)
     * 예: 1등, 총점 15000 → scoreBased ≒ 15, rankBonus 15, participation 2 → 총 32P
     */
    public static int getMultiGamePoint(int finalRank, double totalScore) {
        int scoreBasedPoint = (int) Math.floor(totalScore * MULTI_SCORE_FACTOR);
        int rankBonus = getRankBonus(finalRank);
        return scoreBasedPoint + rankBonus + DEFAULT_PARTICIPATION_POINT;
    }

    private static int getRankBonus(int rank) {
        if (rank <= 0) {
            return 0;
        }
        if (rank <= RANK_BONUS_POINTS.length) {
            return RANK_BONUS_POINTS[rank - 1];
        }
        return 1; // 6등 이후는 소량 보너스
    }
}
