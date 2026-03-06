package com.kospot.multi.player.presentation.response;

import com.kospot.multi.player.domain.entity.GamePlayer;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GamePlayerResponse {

    private Long memberId;
    private Long playerId;
    private String nickname;
    private String markerImageUrl;
    private double totalScore;
    private double roundRank;
    private String gamePlayerStatus;

    public static GamePlayerResponse from(GamePlayer gamePlayer) {
        return GamePlayerResponse.builder()
                .memberId(gamePlayer.getMember().getId())
                .playerId(gamePlayer.getId())
                .nickname(gamePlayer.getNickname())
                .markerImageUrl(gamePlayer.getEquippedMarkerImageUrl())
                .totalScore(gamePlayer.getTotalScore())
                .roundRank(gamePlayer.getRoundRank() != null ? gamePlayer.getRoundRank() : 0)
                .gamePlayerStatus(gamePlayer.getStatus().name())
                .build();
    }

}
