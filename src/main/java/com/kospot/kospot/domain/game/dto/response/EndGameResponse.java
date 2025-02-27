package com.kospot.kospot.domain.game.dto.response;

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

        private double score;

        //todo refactor
        public static RoadViewRank from(RoadViewGame game){
            return RoadViewRank.builder()
                    .score(game.getScore())
                    .build();
        }
    }
}
