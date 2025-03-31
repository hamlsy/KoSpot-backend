package com.kospot.presentation.multiGame.gameRoom.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DetailGameRoomResponse {

    private String title;
    private String gameMode;
    private String gameType;
    private int maxPlayers;

}
