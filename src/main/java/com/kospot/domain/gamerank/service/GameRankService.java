package com.kospot.domain.gamerank.service;

import com.kospot.domain.game.entity.Game;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.gamerank.util.RatingScoreCalculator;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class GameRankService {

    private static final int RECOVERY_SCORE = 100;
    private final EntityManager em;

    public void applyPenaltyForAbandon(GameRank gameRank) {
        gameRank.applyPenaltyForAbandon();
    }

    public void updateRatingScoreAfterGameEnd(GameRank gameRank, Game game) {
        int currentRatingScore = gameRank.getRatingScore() + RECOVERY_SCORE;
        double gameScore = game.getScore();
        int changeRatingScore = RatingScoreCalculator.calculateRatingChange(gameScore, currentRatingScore);

        // gameRank update
        gameRank.changeRatingScore(RECOVERY_SCORE + changeRatingScore);
    }

    /**
     * 전체 랭킹 중 상위 몇 퍼센트인지 계산
     * @param higherRankCount 나보다 높은 랭크 수
     * @param totalRankCount 전체 랭크 수
     * @return 상위 퍼센트 (소수점 첫째 자리까지)
     */
    public double calculateRankPercentage(long higherRankCount, long totalRankCount) {
        if (totalRankCount == 0) {
            return 0.0;
        }
        // 내 순위 = 나보다 높은 랭크 수 + 1
        long myRank = higherRankCount + 1;
        return Math.round((double) myRank / totalRankCount * 1000.0) / 10.0;
    }

}
