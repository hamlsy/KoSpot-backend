package com.kospot.kospot.domain.gameRank.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RankLevel {
    ONE(800), TWO(600), THREE(400), FOUR(200), FIVE(0);

    private final int minRating;

    public static RankLevel getLevelByRating(int rating) {
        int baseRating = rating % 1000;
        for (RankLevel level : values()) {
            if (baseRating >= level.minRating) return level;
        }
        return FIVE; // Default level
    }
}
