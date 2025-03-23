package com.kospot.gameRank.util;

import com.kospot.domain.gameRank.util.RatingScoreCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RatingScoreCalculatorTest {

    @Test
    @DisplayName("레이팅 점수 계산 테스트")
    void calculate_rating_score_test() {
        //given
        int currentRatingScore = 3000;
        int gameScore = 900;

        //when
        int resultRatingScore = RatingScoreCalculator.calculateRatingChange(gameScore, currentRatingScore);

        //then
        System.out.println(resultRatingScore);
    }
}
