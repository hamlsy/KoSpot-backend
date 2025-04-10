package com.kospot.presentation.multiGame.game.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import com.kospot.domain.multiGame.game.entity.MultiRoadViewGame;
import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import com.kospot.presentation.multiGame.gamePlayer.dto.response.GamePlayerResponse;
import com.kospot.presentation.multiGame.round.dto.response.GameRoundResponse;
import lombok.*;

public class MultiRoadViewGameResponse {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Start {

        private Long gameId;
        private int totalRounds;
        private int currentRound;

        private GameRoundResponse.RoadViewInfo roundInfo;
        private List<GamePlayerResponse> gamePlayers;

        public static Start from(MultiRoadViewGame game, RoadViewGameRound round, List<GamePlayer> players) {
            return Start.builder()
                    .gameId(game.getId())
                    .totalRounds(game.getTotalRounds())
                    .currentRound(game.getCurrentRound())
                    .roundInfo(GameRoundResponse.RoadViewInfo.from(round))
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

        private GameRoundResponse.RoadViewInfo roundInfo;
        private List<GamePlayerResponse> gamePlayers;

        public static NextRound from(MultiRoadViewGame game, RoadViewGameRound round, List<GamePlayer> players) {
            return NextRound.builder()
                    .gameId(game.getId())
                    .currentRound(game.getCurrentRound())
                    .roundInfo(GameRoundResponse.RoadViewInfo.from(round))
                    .gamePlayers(
                            players.stream().map(GamePlayerResponse::from).collect(Collectors.toList())
                    )
                    .build();
        }

    }


}
