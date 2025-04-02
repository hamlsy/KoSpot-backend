package com.kospot.domain.multiGame.game.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScoreRule {
    FIRST_PLACE(10),
    SECOND_PLACE(7),
    THIRD_PLACE(5),
    FOURTH_PLACE(3);

    private final int score;

    public static int getScoreByRank(int rank) {
        return switch (rank) {
            case 1 -> FIRST_PLACE.getScore();
            case 2 -> SECOND_PLACE.getScore();
            case 3 -> THIRD_PLACE.getScore();
            case 4 -> FOURTH_PLACE.getScore();
            default -> 0;
        };
    }
} 