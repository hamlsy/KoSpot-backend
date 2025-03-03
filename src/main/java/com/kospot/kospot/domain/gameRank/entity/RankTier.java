package com.kospot.kospot.domain.gameRank.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RankTier {
    BRONZE(1.0, 200, 0, 200),
    SILVER(1.2, 300, 150, 160),
    GOLD(1.5, 350, 200, 120),
    PLATINUM(1.8, 400, 300, 100),
    DIAMOND(2.0, 450, 400, 80),
    MASTER(2.5, 550, 450, 60),
    ;

    private final double pointMultiplier;
    private final int minScoreThreshold;     // 기준 점수
    private final int penaltyThreshold;      // 패널티 기준점
    private final int maxBonus;

    public static RankTier getRankByRating(int rating) {
        if (rating < 1000) return BRONZE;
        if (rating < 2000) return SILVER;
        if (rating < 3000) return GOLD;
        if (rating < 4000) return PLATINUM;
        if (rating < 5000) return DIAMOND;
        return MASTER;
    }
}
