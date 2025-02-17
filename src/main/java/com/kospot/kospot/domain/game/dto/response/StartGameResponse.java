package com.kospot.kospot.domain.game.dto.response;

import com.kospot.kospot.domain.game.entity.RoadViewGame;
import lombok.Builder;
import lombok.Getter;

public class StartGameResponse {

    @Getter
    @Builder
    public static class RoadView {

        private Long gameId;
        private double targetLat;
        private double targetLng;

        public static RoadView from(RoadViewGame game){
            return RoadView.builder()
                    .gameId(game.getId())
                    .targetLat(game.getTargetLat())
                    .targetLng(game.getTargetLng())
                    .build();
        }

    }

}
