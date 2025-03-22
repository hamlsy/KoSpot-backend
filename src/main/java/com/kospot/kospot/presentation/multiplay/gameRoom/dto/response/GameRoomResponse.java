package com.kospot.kospot.presentation.multiplay.gameRoom.dto.response;

import com.kospot.kospot.domain.multiplay.gameRoom.entity.GameRoom;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GameRoomResponse {

    private Long gameRoomId;
    private String title;
    private String gameMode;
    private String gameType;
    private int maxPlayers;

    public static GameRoomResponse from(GameRoom gameRoom) {
        return GameRoomResponse.builder()
                .gameRoomId(gameRoom.getId())
                .title(gameRoom.getTitle())
                .maxPlayers(gameRoom.getMaxPlayers())
                .gameMode(gameRoom.getGameMode().getMode())
                .gameType(gameRoom.getGameType().getType())
                .build();
    }

}
