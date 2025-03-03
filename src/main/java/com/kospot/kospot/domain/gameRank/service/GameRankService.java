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

    private static final int ABANDON_PENALTY = -100;
    private static final int RECOVERY_SCORE = 100;

    public void applyPenaltyForAbandon(GameRank gameRank) {
        gameRank.changeRatingScore(ABANDON_PENALTY);
    }

    public int updateRatingScoreAfterGameEnd(GameRank gameRank, Game game) {
        int ratingScore = gameRank.getRatingScore();
        double gameScore = game.getScore();
        int changedRatingScore = RatingScoreCalculator.calculateRatingChange(gameScore, ratingScore);

        gameRank.changeRatingScore(RECOVERY_SCORE + changedRatingScore);

        return changedRatingScore;
    }

}
