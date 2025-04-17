package com.kospot.presentation.multiGame.round.dto.response;

import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multiGame.submission.entity.roadView.RoadViewPlayerSubmission;
import com.kospot.presentation.multiGame.gamePlayer.dto.response.GamePlayerResponse;
import com.kospot.presentation.multiGame.submission.dto.response.SubmissionResponse;
import lombok.*;

import java.util.List;

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
                    .roundNumber(round.getCurrentRound())
                    .targetLat(round.getTargetCoordinate().getLat())
                    .targetLng(round.getTargetCoordinate().getLng())
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class RoadViewPlayerRoundResult {
        private int roundNumber;
        private double targetLat;
        private double targetLng;
        private List<SubmissionResponse.RoadViewPlayer> playerSubmissionResults;
        private List<GamePlayerResponse> playerTotalResults;

        public static RoadViewPlayerRoundResult from(RoadViewGameRound round,
                                                     List<RoadViewPlayerSubmission> submissions,
                                                     List<GamePlayer> players) {
            return RoadViewPlayerRoundResult.builder()
                    .roundNumber(round.getCurrentRound())
                    .targetLat(round.getTargetCoordinate().getLat())
                    .targetLng(round.getTargetCoordinate().getLng())
                    .playerSubmissionResults(
                            submissions.stream()
                                    .map(SubmissionResponse.RoadViewPlayer::from)
                                    .toList()
                    )
                    .playerTotalResults(
                            players.stream()
                                    .map(GamePlayerResponse::from)
                                    .toList()
                    )
                    .build();
        }

    }

}
