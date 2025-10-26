package com.kospot.domain.point.util;

import com.kospot.domain.gamerank.vo.RankTier;

public class PointCalculator {

    private static final int BASE_PRACTICE_POINT = 50;
    private static final int BASE_RANK_POINT = 100;
    private static final int BASE_MULTI_GAME_POINT = 100;
    private static final double BASE_POINT_ADJUSTMENT = 0.1;
    
    // 멀티 게임 순위별 포인트 (1등~5등 이후)
    private static final int[] RANK_BONUS_POINTS = {150, 100, 70, 50, 30};
    private static final int DEFAULT_PARTICIPATION_POINT = 20;

    public static int getPracticePoint(double score){
        return (int) (getAdjustedPoint(score) * BASE_POINT_ADJUSTMENT + BASE_PRACTICE_POINT);
    }

    public static int getRankPoint(RankTier tier, double score) {
        double basePoint = (getAdjustedPoint(score) + BASE_RANK_POINT) * tier.getPointMultiplier();
        return (int) basePoint;
    }
    
    /**
     * 멀티 게임 포인트 계산
     * @param finalRank 최종 순위 (1등, 2등, ...)
     * @param totalScore 총 점수
     * @return 지급할 포인트
     */
    public static int getMultiGamePoint(int finalRank, double totalScore) {
        // 기본 점수 계산 (점수 비례)
        int scoreBasedPoint = (int) (getAdjustedPoint(totalScore) * BASE_POINT_ADJUSTMENT);
        
        // 순위 보너스 포인트
        int rankBonus = getRankBonus(finalRank);
        
        // 참여 포인트
        int participationPoint = DEFAULT_PARTICIPATION_POINT;
        
        return scoreBasedPoint + rankBonus + participationPoint;
    }
    
    private static int getRankBonus(int rank) {
        if (rank <= 0) {
            return 0;
        }
        if (rank <= RANK_BONUS_POINTS.length) {
            return RANK_BONUS_POINTS[rank - 1];
        }
        return 10; // 6등 이후는 소량 보너스
    }

    private static double getAdjustedPoint(double score){
        return score * BASE_POINT_ADJUSTMENT;
    }
}
