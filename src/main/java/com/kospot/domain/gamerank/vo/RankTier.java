package com.kospot.domain.gamerank.vo;

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
        switch (rating / 1000) {
            case 0:
                return BRONZE;
            case 1:
                return SILVER;
            case 2:
                return GOLD;
            case 3:
                return PLATINUM;
            case 4:
                return DIAMOND;
            default:
                return MASTER;
        }
    }
}
