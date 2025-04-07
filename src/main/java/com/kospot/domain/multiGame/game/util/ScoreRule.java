package com.kospot.domain.multiGame.game.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ScoreRule {
    protected final int score;

    // 순위별 점수 (1등부터 8등까지)
    private static final int[] SCORES = {10, 8, 6, 5, 4, 3, 2, 1};

    // 로드뷰 게임 점수 규칙
    public static final int FIRST_PLACE = 100;
    public static final int SECOND_PLACE = 70;
    public static final int THIRD_PLACE = 50;
    public static final int OTHER_PLACE = 30;
    
    // 포토 게임 점수 규칙
    public static final int PHOTO_FIRST_ANSWER = 100;
    public static final int PHOTO_SECOND_ANSWER = 80;
    public static final int PHOTO_THIRD_ANSWER = 60;
    public static final int PHOTO_OTHER_ANSWER = 40;

    public static int calculateScore(int rank) {
        if (rank <= 0) return 0;
        return rank <= SCORES.length ? SCORES[rank - 1] : 0;
    }

    public static int getScoreByPhotoAnswerOrder(int order) {
        switch (order) {
            case 1:
                return PHOTO_FIRST_ANSWER;
            case 2:
                return PHOTO_SECOND_ANSWER;
            case 3:
                return PHOTO_THIRD_ANSWER;
            default:
                return PHOTO_OTHER_ANSWER;
        }
    }
} 