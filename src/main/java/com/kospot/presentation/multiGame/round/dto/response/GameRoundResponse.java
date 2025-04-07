package com.kospot.presentation.multiGame.round.dto.response;

import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import lombok.*;

public class GameRoundResponse {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class RoadViewInfo {
        private Long roundId;
        private int roundNumber;
        private double targetLat;
        private double targetLng;

        public static RoadViewInfo from(RoadViewGameRound round) {
            return RoadViewInfo.builder()
                    .roundId(round.getId())
                    .roundNumber(round.getRoundNumber())
                    .targetLat(round.getTargetCoordinate().getLat())
                    .targetLng(round.getTargetCoordinate().getLng())
                    .build();
        }
    }
}
