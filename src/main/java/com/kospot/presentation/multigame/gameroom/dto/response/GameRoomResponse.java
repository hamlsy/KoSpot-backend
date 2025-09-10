package com.kospot.presentation.multigame.gameroom.dto.response;

import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
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
    private String playerMatchType;
    private int maxPlayers;

    public static GameRoomResponse from(GameRoom gameRoom) {
        return GameRoomResponse.builder()
                .gameRoomId(gameRoom.getId())
                .title(gameRoom.getTitle())
                .maxPlayers(gameRoom.getMaxPlayers())
                .gameMode(gameRoom.getGameMode().getMode())
                .playerMatchType(gameRoom.getPlayerMatchType().getType())
                .build();
    }

}
