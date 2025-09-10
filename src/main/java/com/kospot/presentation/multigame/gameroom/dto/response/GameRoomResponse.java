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
    private String gameModeKey;
    private String playerMatchTypeKey;
    private int maxPlayers;

    public static GameRoomResponse from(GameRoom gameRoom) {
        return GameRoomResponse.builder()
                .gameRoomId(gameRoom.getId())
                .title(gameRoom.getTitle())
                .maxPlayers(gameRoom.getMaxPlayers())
                .gameModeKey(gameRoom.getGameMode().name())
                .playerMatchTypeKey(gameRoom.getPlayerMatchType().name())
                .build();
    }

}
