package com.kospot.kospot.domain.rank.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RankLevel {
    FIVE(0), FOUR(100), THREE(200), TWO(300), ONE(400);

    private final int minRating;

    public static RankLevel getLevelByRating(int rating) {
        int baseRating = rating % 500;
        for (RankLevel level : values()) {
            if (baseRating >= level.minRating) return level;
        }
        return FIVE; // Default level
    }
}
