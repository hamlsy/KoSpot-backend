package com.kospot.domain.gamerank.util;

import com.kospot.domain.gamerank.vo.RankTier;

import java.util.EnumMap;
import java.util.Map;

public class RatingScoreCalculator {

    /**
     * 점수 예시 (Diamond 기준):
     * - 460점 → 조정 임계값(450+10) 대비 +0 → ±0
     * - 500점 → 초과 40, gainDivisor=6 → +6 (최대 보너스 80 이하)
     * - 430점 → 부족 30, penaltyUnit=8 → -4 (최소 패널티 3 이상)
     */
    private static final int MIN_GAME_SCORE = 0;
    private static final int MAX_GAME_SCORE = 1000;

    private static final Map<RankTier, TierAdjustment> TIER_ADJUSTMENTS = new EnumMap<>(RankTier.class);

    static {
        TIER_ADJUSTMENTS.put(RankTier.BRONZE, new TierAdjustment(20, 5.0, Double.POSITIVE_INFINITY, 0));
        TIER_ADJUSTMENTS.put(RankTier.SILVER, new TierAdjustment(30, 6.0, 12.0, 2));
        TIER_ADJUSTMENTS.put(RankTier.GOLD, new TierAdjustment(40, 7.0, 10.0, 3));
        TIER_ADJUSTMENTS.put(RankTier.PLATINUM, new TierAdjustment(50, 7.5, 9.0, 4));
        TIER_ADJUSTMENTS.put(RankTier.DIAMOND, new TierAdjustment(60, 8.0, 8.0, 4));
        TIER_ADJUSTMENTS.put(RankTier.MASTER, new TierAdjustment(70, 9.0, 7.0, 5));
    }

    /**
     * 게임 점수를 기반으로 얻게 될 레이팅 포인트를 계산합니다.
     *
     * @param gameScore 게임 한 판의 점수 (0~1000)
     * @param currentRatingScore 현재 사용자의 레이팅 점수
     * @return 변경될 레이팅 포인트 (양수 또는 음수)
     */
    public static int calculateRatingChange(double gameScore, int currentRatingScore) {
        double clampedScore = clampGameScore(gameScore);
        RankTier tier = RankTier.getRankByRating(currentRatingScore);
        TierAdjustment adjustment = TIER_ADJUSTMENTS.getOrDefault(tier, TierAdjustment.DEFAULT);

        double adjustedThreshold = tier.getMinScoreThreshold() + adjustment.positiveMargin();
        if (clampedScore >= adjustedThreshold) {
            // 초과분이 작으면 0 또는 작은 포인트만 지급되도록 gainDivisor 확장
            double overshoot = clampedScore - adjustedThreshold;
            int gained = (int) Math.floor(overshoot / adjustment.gainDivisor());
            return Math.min(gained, tier.getMaxBonus());
        }

        // 기준 미달 시 패널티 계산 (브론즈 예외)
        if (tier == RankTier.BRONZE) {
            return 0;
        }

        double deficit = adjustedThreshold - clampedScore;
        int penalty = (int) Math.ceil(deficit / adjustment.penaltyUnit());
        penalty = Math.max(penalty, adjustment.minPenalty());
        return -penalty;
    }

    private static double clampGameScore(double gameScore) {
        if (gameScore < MIN_GAME_SCORE) {
            return MIN_GAME_SCORE;
        }
        if (gameScore > MAX_GAME_SCORE) {
            return MAX_GAME_SCORE;
        }
        return gameScore;
    }

    private record TierAdjustment(int positiveMargin,
                                  double gainDivisor,
                                  double penaltyUnit,
                                  int minPenalty) {

        private static final TierAdjustment DEFAULT = new TierAdjustment(30, 7.0, 10.0, 3);

        private TierAdjustment {
            if (penaltyUnit <= 0 && penaltyUnit != Double.POSITIVE_INFINITY) {
                throw new IllegalArgumentException("penaltyUnit must be positive or POSITIVE_INFINITY");
            }
        }
    }
}
