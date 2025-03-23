package com.kospot.presentation.multiGame.gameRoom.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class FindGameRoomResponse {

    private Long gameRoomId;
    private String title;
    private String gameMode;
    private String gameType;
    private int maxPlayers;
    private int currentPlayers;
    private String hostNickname;
    private boolean privateRoom;
    private String gameRoomStatus;

}
