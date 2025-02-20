package com.kospot.kospot.domain.gameRank.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RankTier {
    BRONZE(1.0),
    SILVER(1.2),
    GOLD(1.5),
    PLATINUM(1.8),
    DIAMOND(2.0),
    MASTER(2.5),
    ;

    private final double pointMultiplier;

    public static RankTier getRankByRating(int rating) {
        if (rating < 500) return BRONZE;
        if (rating < 1000) return SILVER;
        if (rating < 1500) return GOLD;
        if (rating < 2000) return PLATINUM;
        if (rating < 2500) return DIAMOND;
        return MASTER;
    }
}
