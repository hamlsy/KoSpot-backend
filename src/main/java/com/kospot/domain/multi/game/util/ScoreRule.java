package com.kospot.domain.multi.game.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ScoreRule {
    protected final int score;

    // 순위별 점수 (1등부터 8등까지)
    private static final int[] SCORES = {10, 8, 6, 5, 4, 3, 2, 1};

    public static int calculateScore(int rank) {
        if (rank <= 0) return 0;
        return rank <= SCORES.length ? SCORES[rank - 1] : 0;
    }

} 