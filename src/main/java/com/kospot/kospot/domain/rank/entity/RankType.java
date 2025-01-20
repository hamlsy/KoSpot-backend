package com.kospot.kospot.domain.rank.entity;

public enum RankType {
    BRONZE, SILVER, GOLD, DIAMOND, MASTER;

    public static RankType getRankByRating(int rating) {
        if (rating < 500) return BRONZE;
        if (rating < 1000) return SILVER;
        if (rating < 1500) return GOLD;
        if (rating < 2000) return DIAMOND;
        return MASTER;
    }
}
