package com.kospot.presentation.game.dto.response;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.gamerank.vo.RankLevel;
import com.kospot.domain.gamerank.vo.RankTier;
import lombok.Builder;
import lombok.Getter;

public class EndGameResponse {

    @Getter
    @Builder
    public static class RoadViewPractice {
        private String fullAddress;
        private String poiName;
        private double score;

        public static RoadViewPractice from(RoadViewGame game, Coordinate coordinate){
            return RoadViewPractice.builder()
                    .score(game.getScore())
                    .fullAddress(coordinate.getAddress().getFullAddress())
                    .poiName(coordinate.getPoiName())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class RoadViewRank {
        private String fullAddress;
        private String poiName;
        private double score;
        private int previousRatingScore;
        private int currentRatingScore;
        private int ratingScoreChange;
        private RankTier previousRankTier;
        private RankLevel previousRankLevel;
        private RankTier currentRankTier;
        private RankLevel currentRankLevel;

        public static RoadViewRank from(RoadViewGame game,  Coordinate coordinate, int previousRatingScore, GameRank currentGameRank){
            int currentRatingScore = currentGameRank.getRatingScore();
            int ratingScoreChange = currentRatingScore - previousRatingScore;

            RankTier previousRankTier = RankTier.getRankByRating(previousRatingScore);
            RankLevel previousRankLevel = RankLevel.getLevelByRating(previousRatingScore);

            return RoadViewRank.builder()
                    .score(game.getScore())
                    .fullAddress(coordinate.getAddress().getFullAddress())
                    .poiName(coordinate.getPoiName())
                    .previousRatingScore(previousRatingScore)
                    .currentRatingScore(currentRatingScore)
                    .ratingScoreChange(ratingScoreChange)
                    .previousRankTier(previousRankTier)
                    .previousRankLevel(previousRankLevel)
                    .currentRankTier(currentGameRank.getRankTier())
                    .currentRankLevel(currentGameRank.getRankLevel())
                    .build();
        }
    }
}
