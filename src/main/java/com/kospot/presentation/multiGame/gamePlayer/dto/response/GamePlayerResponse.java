package com.kospot.presentation.multiGame.gamePlayer.dto.response;

import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GamePlayerResponse {

    private Long playerId;
    private String nickname;
    private String markerImageUrl;
    private int totalScore;
    private int roundRank;

    public static GamePlayerResponse from(GamePlayer gamePlayer) {
        return GamePlayerResponse.builder()
                .playerId(gamePlayer.getId())
                .nickname(gamePlayer.getNickname())
                .markerImageUrl(gamePlayer.getEquippedMarkerImageUrl())
                .totalScore(gamePlayer.getTotalScore())
                .roundRank(gamePlayer.getRoundRank())
                .build();
    }

}
