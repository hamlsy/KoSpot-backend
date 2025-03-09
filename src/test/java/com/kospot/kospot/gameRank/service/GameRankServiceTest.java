package com.kospot.kospot.gameRank.service;

import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.gameRank.entity.RankLevel;
import com.kospot.kospot.domain.gameRank.entity.RankTier;
import com.kospot.kospot.domain.gameRank.service.GameRankService;
import com.kospot.kospot.domain.gameRank.util.RatingScoreCalculator;
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
        assertEquals(changeRatingScore, game.getRatingScoreChange());
        System.out.println("game.getChangeRatingScore() = " + game.getRatingScoreChange());

    }

    @Test
    @DisplayName("게임 랭크 테스트")
    void testGameRank(){
        //given
        GameRank rank = GameRank.builder()
                .ratingScore(3000)
                .build();
        rank.applyPenaltyForAbandon();

        RoadViewGame game = RoadViewGame.builder()
                .score(100)
                .build();
        //when
        gameRankService.updateRatingScoreAfterGameEnd(rank,game);

        //then
        System.out.println("rank.getRankTier() = " + rank.getRankTier());
        System.out.println("rank.getRankLevel() = " + rank.getRankLevel());
        System.out.println(rank.getRatingScore());
        System.out.println(RatingScoreCalculator.calculateRatingChange(100, 3000));

        assertEquals(RankTier.GOLD, rank.getRankTier());
        assertEquals(RankLevel.ONE, rank.getRankLevel());

    }
}
