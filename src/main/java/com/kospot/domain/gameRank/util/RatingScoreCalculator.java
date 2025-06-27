package com.kospot.domain.gameRank.util;

import com.kospot.domain.gameRank.vo.RankTier;

public class RatingScoreCalculator {

    // 기본 점수 범위 (0~1000)
    private static final int MIN_GAME_SCORE = 0;
    private static final int MAX_GAME_SCORE = 1000;
    private static final double PENALTY_COEFFICIENT = 7.0;
    public static final double BASE_POINT_COEFFICIENT = 5.0;

    /**
     * 게임 점수를 기반으로 얻게 될 레이팅 포인트를 계산합니다.
     *
     * @param gameScore 게임 한 판의 점수 (0~1000)
     * @param currentRatingScore 현재 사용자의 레이팅 점수
     * @return 변경될 레이팅 포인트 (양수 또는 음수)
     */
    public static int calculateRatingChange(double gameScore, int currentRatingScore) {
        // 게임 점수 범위 검증
        if (gameScore < MIN_GAME_SCORE) {
            gameScore = MIN_GAME_SCORE;
        } else if (gameScore > MAX_GAME_SCORE) {
            gameScore = MAX_GAME_SCORE;
        }

        RankTier tier = RankTier.getRankByRating(currentRatingScore);

        // 점수 평가: 포인트 계산
        int ratingChange = 0;
        if (gameScore >= tier.getMinScoreThreshold()) {
            // 기준 점수를 넘겼을 때 포인트 계산
            double basePoints = (gameScore - tier.getMinScoreThreshold()) / BASE_POINT_COEFFICIENT;

            // 포인트 계산 (티어가 높을수록 얻는 양 감소)
            ratingChange = (int) Math.min(tier.getMaxBonus(), basePoints);
        } else {
            // 패널티 점수 계산 (티어에 따라 다름)
            ratingChange = calculatePenalty(gameScore, tier);
        }

        return ratingChange;
    }

    /**
     * 기준 점수 미만일 경우 패널티(마이너스 점수)를 계산합니다.
     * 브론즈의 경우 패널티가 없습니다.
     */
    private static int calculatePenalty(double gameScore, RankTier tier) {
        // 브론즈는 패널티 없음
        if (tier == RankTier.BRONZE) {
            return 0;
        }

        // 패널티 임계값 이상이면 패널티 없음 (하지만 보너스도 없음)
        if (gameScore >= tier.getPenaltyThreshold()) {
            return 0;
        }

        // 패널티 계산 (티어가 높을수록 패널티 심화)
        int penalty = (int) ((tier.getPenaltyThreshold() - gameScore) / PENALTY_COEFFICIENT);

        // 패널티는 음수로 반환
        return -penalty;
    }

    /**
     * 새로운 레이팅 포인트를 계산합니다.
     *
     * @param currentRating 현재 레이팅
     * @param ratingChange 변경될 레이팅 포인트
     * @return 최종 레이팅 포인트
     */
    private static int calculateNewRating(int currentRating, int ratingChange) {
        return Math.max(0, currentRating + ratingChange); // 레이팅은 0 아래로 내려가지 않음
    }

    /**
     * 게임 점수로부터 최종 레이팅 점수를 계산합니다.
     *
     * @param gameScore 게임 한 판의 점수
     * @param currentRating 현재 레이팅
     * @return 계산된 최종 레이팅
     */
    private static int calculateRating(int gameScore, int currentRating) {
        int ratingChange = calculateRatingChange(gameScore, currentRating);
        return calculateNewRating(currentRating, ratingChange);
    }
}
