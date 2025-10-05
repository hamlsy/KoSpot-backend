package com.kospot.presentation.multigame.game.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.presentation.multigame.gamePlayer.dto.response.GamePlayerResponse;
import com.kospot.presentation.multigame.round.dto.response.RoadViewRoundResponse;
import lombok.*;

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
        private List<GamePlayerResponse> gamePlayers;

        public static StartPlayerGame from(MultiRoadViewGame game, RoadViewGameRound round, List<GamePlayer> players) {
            return StartPlayerGame.builder()
                    .gameId(game.getId())
                    .totalRounds(game.getTotalRounds())
                    .currentRound(game.getCurrentRound())
                    .roundInfo(RoadViewRoundResponse.Info.from(round))
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
        private int currentRound;

        private RoadViewRoundResponse.Info roundInfo;

        public static NextRound from(MultiRoadViewGame game, RoadViewGameRound round) {
            return NextRound.builder()
                    .gameId(game.getId())
                    .currentRound(game.getCurrentRound())
                    .roundInfo(RoadViewRoundResponse.Info.from(round))
                    .build();
        }

    }


}
