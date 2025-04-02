package com.kospot.presentation.multiGame.gameRoom.dto.response;

import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
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
    private boolean privateRoom;
    private int maxPlayers;

    public static GameRoomDetailResponse from(GameRoom gameRoom) {
        return GameRoomDetailResponse.builder()
                .title(gameRoom.getTitle())
                .gameMode(gameRoom.getGameMode().name())
                .gameType(gameRoom.getPlayerMatchType().name())
                .maxPlayers(gameRoom.getMaxPlayers())
                .privateRoom(gameRoom.isPrivateRoom())
                .build();
    }

}
