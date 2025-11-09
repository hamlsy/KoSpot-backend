package com.kospot.presentation.multi.game.dto.response;

import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

public class MultiRoadViewGameResponse {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class RoundPreview {

        private Long gameId;
        private Long roundId;
        private int currentRound;
        private int totalRounds;
        private long roundVersion;
        private double targetLat;
        private double targetLng;
        private int timeLimitSeconds;

        public static RoundPreview from(MultiRoadViewGame game, RoadViewGameRound round, long roundVersion) {
            return RoundPreview.builder()
                    .gameId(game.getId())
                    .roundId(round.getId())
                    .currentRound(game.getCurrentRound())
                    .totalRounds(game.getTotalRounds())
                    .roundVersion(roundVersion)
                    .targetLat(round.getTargetCoordinate().getLat())
                    .targetLng(round.getTargetCoordinate().getLng())
                    .timeLimitSeconds(
                            round.getTimeLimit() != null ? round.getTimeLimit() : game.getTimeLimit()
                    )
                    .build();
        }
    }
}
