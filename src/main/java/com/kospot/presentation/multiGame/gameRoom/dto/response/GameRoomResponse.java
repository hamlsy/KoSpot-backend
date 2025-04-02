package com.kospot.presentation.multiGame.gameRoom.dto.response;

import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
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
                .gameType(gameRoom.getPlayerMatchType().getType())
                .build();
    }

}
