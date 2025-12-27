package com.kospot.domain.gamerank.vo;

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

    public static RankLevel fromKey(String key) {
        // 1이면 one, 2면 two..
        if (key.equals("1")) return ONE;
        if (key.equals("2")) return TWO;
        if (key.equals("3")) return THREE;
        if (key.equals("4")) return FOUR;
        if (key.equals("5")) return FIVE;

        for (RankLevel level : values()) {
            if (level.name().equalsIgnoreCase(key)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid RankLevel key: " + key);
    }
}
