package com.kospot.kospot.presentation.game.dto.response;

import com.kospot.kospot.domain.game.entity.RoadViewGame;
import lombok.Builder;
import lombok.Getter;

public class EndGameResponse {

    @Getter
    @Builder
    public static class RoadViewPractice {
        private double score;

        public static RoadViewPractice from(RoadViewGame game){
            return RoadViewPractice.builder()
                    .score(game.getScore())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class RoadViewRank {

        private int currentRatingPoint;
        private int ratingScoreChange;
        private double score;

        public static RoadViewRank from(RoadViewGame game, int currentRatingPoint, int ratingScoreChange){
            return RoadViewRank.builder()
                    .score(game.getScore())
                    .currentRatingPoint(currentRatingPoint)
                    .ratingScoreChange(ratingScoreChange)
                    .build();
        }
    }
}
