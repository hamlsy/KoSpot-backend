package com.kospot.domain.multiGame.game.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScoreRule {
    FIRST_PLACE(10),
    SECOND_PLACE(7),
    THIRD_PLACE(5),
    FOURTH_PLACE(3),
    
    // 사진 게임 정답 순서별 점수
    PHOTO_FIRST_ANSWER(10),   // 첫 번째 정답자
    PHOTO_SECOND_ANSWER(8),   // 두 번째 정답자
    PHOTO_THIRD_ANSWER(6),    // 세 번째 정답자
    PHOTO_FOURTH_ANSWER(5),   // 네 번째 정답자
    PHOTO_LATER_ANSWER(3);    // 다섯 번째 이상 정답자

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
    
    public static int getScoreByPhotoAnswerOrder(int order) {
        return switch (order) {
            case 1 -> PHOTO_FIRST_ANSWER.getScore();
            case 2 -> PHOTO_SECOND_ANSWER.getScore();
            case 3 -> PHOTO_THIRD_ANSWER.getScore();
            case 4 -> PHOTO_FOURTH_ANSWER.getScore();
            default -> PHOTO_LATER_ANSWER.getScore();
        };
    }
} 