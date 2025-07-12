package com.kospot.presentation.multigame.gameRoom.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GameRoomPlayerResponse {
    
    private String nickname;
    private boolean isHost;
    private String markerImageUrl;
    private String rankTier;
    private int rankLevel;
    private int ratingScore;
    //todo add statistic

}
