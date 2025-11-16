package com.kospot.presentation.multi.round.dto.response;

import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.submission.entity.roadview.RoadViewSubmission;
import com.kospot.presentation.multi.gamePlayer.dto.response.GamePlayerResponse;
import com.kospot.presentation.multi.submission.dto.response.SubmissionResponse;
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
        private String poiName;
        private double targetLat;
        private double targetLng;

        public static Info from(RoadViewGameRound round, MultiRoadViewGame game) {
            return Info.builder()
                    .roundId(round.getId())
                    .roundNumber(round.getRoundNumber())
                    .poiName(
                            game.isPoiNameVisible() ? round.getTargetCoordinate().getPoiName() : null
                    )
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
        private String poiName;
        private String fullAddress;
        private int roundNumber;
        private double targetLat;
        private double targetLng;
        private List<SubmissionResponse.RoadViewPlayer> playerSubmissionResults;
        private List<GamePlayerResponse> playerTotalResults;

        public static PlayerResult from(RoadViewGameRound round,
                                        List<RoadViewSubmission> submissions,
                                        List<GamePlayer> players) {
            return PlayerResult.builder()
                    .poiName(round.getTargetCoordinate().getPoiName())
                    .fullAddress(round.getTargetCoordinate().getAddress().getFullAddress())
                    .roundNumber(round.getRoundNumber()-1) // 이미 add된 라운드 -1
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
