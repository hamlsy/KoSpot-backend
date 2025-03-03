package com.kospot.kospot.domain.gameRank.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RankLevel {
    FIVE(0), FOUR(200), THREE(400), TWO(600), ONE(800);

    private final int minRating;

    public static RankLevel getLevelByRating(int rating) {
        int baseRating = rating % 1000;
        for (RankLevel level : values()) {
            if (baseRating >= level.minRating) return level;
        }
        return FIVE; // Default level
    }
}
