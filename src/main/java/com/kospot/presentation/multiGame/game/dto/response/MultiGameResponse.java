package com.kospot.presentation.multiGame.game.dto.response;

import java.util.List;

import com.kospot.presentation.multiGame.round.dto.response.GameRoundResponse;
import lombok.*;

public class MultiGameResponse {

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
        //todo gameplayer dto
    }

}
