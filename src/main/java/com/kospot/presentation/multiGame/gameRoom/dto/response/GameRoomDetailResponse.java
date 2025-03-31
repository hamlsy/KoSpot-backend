package com.kospot.presentation.multiGame.gameRoom.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GameRoomDetailResponse {

    private String title;
    private String gameMode;
    private String gameType;
    private int maxPlayers;

}
