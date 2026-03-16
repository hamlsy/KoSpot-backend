package com.kospot.game.presentation.dto.response;

import com.kospot.coordinate.domain.entity.Coordinate;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.gamerank.domain.entity.GameRank;
import com.kospot.gamerank.domain.vo.RankLevel;
import com.kospot.gamerank.domain.vo.RankTier;
import com.kospot.member.domain.entity.Member;
import lombok.Builder;
import lombok.Getter;

public class EndGameResponse {

    @Getter
    @Builder
    public static class RoadViewPractice {
        private String nickname;
        private double answerDistance;
        private String fullAddress;
        private String poiName;
        private double score;

        public static RoadViewPractice from(Member member, RoadViewGame game, Coordinate coordinate){
            return RoadViewPractice.builder()
                    .nickname(member.getNickname())
                    .score(game.getScore())
                    .answerDistance(game.getAnswerDistance())
                    .fullAddress(coordinate.getAddress().getFullAddress())
                    .poiName(coordinate.getPoiName())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class RoadViewRank {
        private String nickname;
        private double answerDistance;
        private String fullAddress;
        private String poiName;
        private double baseScore;
        private int bonusScore;
        private double score;
        private int previousRatingScore;
        private int currentRatingScore;
        private int ratingScoreChange;
        private RankTier previousRankTier;
        private RankLevel previousRankLevel;
        private RankTier currentRankTier;
        private RankLevel currentRankLevel;

        public static RoadViewRank from(Member member, RoadViewGame game, Coordinate coordinate, int previousRatingScore, GameRank currentGameRank){
            int currentRatingScore = currentGameRank.getRatingScore();
            int ratingScoreChange = currentRatingScore - previousRatingScore;

            RankTier previousRankTier = RankTier.getRankByRating(previousRatingScore);
            RankLevel previousRankLevel = RankLevel.getLevelByRating(previousRatingScore);

            return RoadViewRank.builder()
                    .nickname(member.getNickname())
                    .baseScore(game.getBaseScore())
                    .bonusScore(game.getBonusScore())
                    .score(game.getScore())
                    .fullAddress(coordinate.getAddress().getFullAddress())
                    .poiName(coordinate.getPoiName())
                    .answerDistance(game.getAnswerDistance())
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
