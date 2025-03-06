package com.kospot.kospot.domain.gameRank.service;

import com.kospot.kospot.domain.game.entity.Game;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.gameRank.util.RatingScoreCalculator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class GameRankService {

    private static final int RECOVERY_SCORE = 100;

    public void applyPenaltyForAbandon(GameRank gameRank) {
        gameRank.applyPenaltyForAbandon();
    }

    public void updateRatingScoreAfterGameEnd(GameRank gameRank, Game game) {
        int currentRatingScore = gameRank.getRatingScore() + RECOVERY_SCORE;
        double gameScore = game.getScore();
        int changeRatingScore = RatingScoreCalculator.calculateRatingChange(gameScore, currentRatingScore);

        game.updateRatingScore(currentRatingScore, changeRatingScore);

        // gameRank update
        gameRank.changeRatingScore(RECOVERY_SCORE + changeRatingScore);

    }

    public void updateRatingScoreAfterGameEndV2(GameRank gameRank, Game game) {
        int currentRatingScore = gameRank.getRatingScore() + RECOVERY_SCORE;
        double gameScore = game.getScore();
        int changeRatingScore = RatingScoreCalculator.calculateRatingChange(gameScore, currentRatingScore);

        // gameRank update
        gameRank.changeRatingScore(RECOVERY_SCORE + changeRatingScore);

    }

}
