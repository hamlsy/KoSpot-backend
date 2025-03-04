package com.kospot.kospot.gameRank.service;

import com.kospot.kospot.domain.game.entity.Game;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.gameRank.service.GameRankService;
import com.kospot.kospot.domain.gameRank.util.RatingScoreCalculator;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class GameRankServiceTest {

    @Autowired
    private GameRankService gameRankService;

    @Test
    @DisplayName("게임 레이팅 점수 테스트")
    void testGameRatingScore(){
        //given
        GameRank rank = GameRank.builder()
                .ratingScore(3000)
                .build();
        RoadViewGame game = RoadViewGame.builder()
                .score(500)
                .build();
        //when
        gameRankService.updateRatingScoreAfterGameEnd(rank, game);

        //then
        int changeRatingScore = RatingScoreCalculator.calculateRatingChange(game.getScore(), rank.getRatingScore());
        assertEquals(3000 + changeRatingScore, rank.getRatingScore() - 100);
        System.out.println("changeRatingScore = " + changeRatingScore);
        assertEquals(3000, game.getCurrentRatingScore());
        System.out.println("game.getCurrentRatingScore() = " + game.getCurrentRatingScore());
        assertEquals(changeRatingScore, game.getChangeRatingScore());
        System.out.println("game.getChangeRatingScore() = " + game.getChangeRatingScore());

    }
}
