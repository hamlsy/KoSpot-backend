package com.kospot.domain.point.util;

import com.kospot.domain.gameRank.vo.RankTier;

public class PointCalculator {

    private static final int BASE_PRACTICE_POINT = 50;
    private static final int BASE_RANK_POINT = 100;
    private static final double BASE_POINT_ADJUSTMENT = 0.1;

    public static int getPracticePoint(double score){
        return (int) (getAdjustedPoint(score) * BASE_POINT_ADJUSTMENT + BASE_PRACTICE_POINT);
    }

    public static int getRankPoint(RankTier tier, double score) {
        double basePoint = (getAdjustedPoint(score) + BASE_RANK_POINT) * tier.getPointMultiplier();
        return (int) basePoint;
    }

    private static double getAdjustedPoint(double score){
        return score * BASE_POINT_ADJUSTMENT;
    }
}
