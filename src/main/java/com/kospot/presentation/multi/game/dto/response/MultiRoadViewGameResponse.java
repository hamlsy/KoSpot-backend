package com.kospot.presentation.multi.game.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.presentation.multi.gamePlayer.dto.response.GamePlayerResponse;
import com.kospot.presentation.multi.round.dto.response.RoadViewRoundResponse;
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
    public static class StartPlayerGame {

        private Long gameId;
        private int totalRounds;
        private int currentRound;

        private RoadViewRoundResponse.Info roundInfo;
        private long roundVersion;
        private List<GamePlayerResponse> gamePlayers;

        public static StartPlayerGame from(MultiRoadViewGame game, RoadViewGameRound round,
                                           List<GamePlayer> players, long roundVersion) {
            return StartPlayerGame.builder()
                    .gameId(game.getId())
                    .totalRounds(game.getTotalRounds())
                    .currentRound(game.getCurrentRound())
                    .roundVersion(roundVersion)
                    .roundInfo(RoadViewRoundResponse.Info.from(round, game))
                    .gamePlayers(
                            players.stream().map(GamePlayerResponse::from).collect(Collectors.toList())
                    )
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class NextRound {

        private Long gameId;
        private RoadViewRoundResponse.Info roundInfo;
        private int currentRound;
        private long roundVersion;

        public static NextRound from(MultiRoadViewGame game, RoadViewGameRound round, long roundVersion) {
            return NextRound.builder()
                    .gameId(game.getId())
                    .currentRound(game.getCurrentRound())
                    .roundInfo(RoadViewRoundResponse.Info.from(round, game))
                    .roundVersion(roundVersion)
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class RoundProblem {

        private Long gameId;
        private Long roundId;
        private long roundVersion;
        private double targetLat;
        private double targetLng;

        public static RoundProblem from(MultiRoadViewGame game, RoadViewGameRound round, long roundVersion) {
            return RoundProblem.builder()
                    .gameId(game.getId())
                    .roundId(round.getId())
                    .roundVersion(roundVersion)
                    .targetLat(round.getTargetCoordinate().getLat())
                    .targetLng(round.getTargetCoordinate().getLng())
                    .build();
        }
    }
}
