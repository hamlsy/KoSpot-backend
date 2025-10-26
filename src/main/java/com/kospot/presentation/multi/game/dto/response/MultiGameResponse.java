package com.kospot.presentation.multi.game.dto.response;

import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.point.util.PointCalculator;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

public class MultiGameResponse {

    /**
     * 게임 최종 결과 (WebSocket으로 전송)
     */
    @Getter
    @Builder
    public static class GameFinalResult {
        private Long gameId;
        private String message;
        private Long timestamp;
        private List<PlayerFinalResult> playerResults;

        public static GameFinalResult from(Long gameId, List<GamePlayer> gamePlayers) {
            List<PlayerFinalResult> playerResults = gamePlayers.stream()
                    .map(PlayerFinalResult::from)
                    .collect(Collectors.toList());

            return GameFinalResult.builder()
                    .gameId(gameId)
                    .message("게임이 종료되었습니다.")
                    .timestamp(System.currentTimeMillis())
                    .playerResults(playerResults)
                    .build();
        }
    }

    /**
     * 플레이어별 최종 결과
     */
    @Getter
    @Builder
    public static class PlayerFinalResult {
        private Long playerId;
        private String nickname;
        private String markerImageUrl;
        private Double totalScore;
        private Integer finalRank;      // roundRank가 최종 순위
        private Integer earnedPoint;    // 획득할 포인트

        public static PlayerFinalResult from(GamePlayer gamePlayer) {
            int finalRank = gamePlayer.getRoundRank() != null ? gamePlayer.getRoundRank() : 999;
            int earnedPoint = PointCalculator.getMultiGamePoint(finalRank, gamePlayer.getTotalScore());

            return PlayerFinalResult.builder()
                    .playerId(gamePlayer.getId())
                    .nickname(gamePlayer.getNickname())
                    .markerImageUrl(gamePlayer.getEquippedMarkerImageUrl())
                    .totalScore(gamePlayer.getTotalScore())
                    .finalRank(finalRank)
                    .earnedPoint(earnedPoint)
                    .build();
        }
    }
}

