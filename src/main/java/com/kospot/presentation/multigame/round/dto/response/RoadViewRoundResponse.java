package com.kospot.presentation.multigame.round.dto.response;

import com.kospot.domain.multigame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multigame.round.entity.RoadViewGameRound;
import com.kospot.domain.multigame.submission.entity.roadView.RoadViewPlayerSubmission;
import com.kospot.presentation.multigame.gamePlayer.dto.response.GamePlayerResponse;
import com.kospot.presentation.multigame.submission.dto.response.SubmissionResponse;
import lombok.*;

import java.util.List;

public class RoadViewRoundResponse {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Info {
        private Long roundId;
        private int roundNumber;
        private double targetLat;
        private double targetLng;

        public static Info from(RoadViewGameRound round) {
            return Info.builder()
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
    public static class PlayerResult {
        private int roundNumber;
        private double targetLat;
        private double targetLng;
        private List<SubmissionResponse.RoadViewPlayer> playerSubmissionResults;
        private List<GamePlayerResponse> playerTotalResults;

        public static PlayerResult from(RoadViewGameRound round,
                                        List<RoadViewPlayerSubmission> submissions,
                                        List<GamePlayer> players) {
            return PlayerResult.builder()
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
