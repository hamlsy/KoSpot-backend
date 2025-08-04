package com.kospot.presentation.multigame.gameroom.dto.response;

import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
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
    private int currentPlayerCount; //todo redis
    private String hostNickname;
    private boolean privateRoom;
    private String gameRoomStatus;

    public static FindGameRoomResponse from(GameRoom gameRoom) {
        return FindGameRoomResponse.builder()
                .gameRoomId(gameRoom.getId())
                .title(gameRoom.getTitle())
                .gameMode(gameRoom.getGameMode().name())
                .gameType(gameRoom.getPlayerMatchType().name())
                .maxPlayers(gameRoom.getMaxPlayers())
                .hostNickname(gameRoom.getHost().getNickname())
                .privateRoom(gameRoom.isPrivateRoom())
                .gameRoomStatus(gameRoom.getStatus().name())
                .build();
    }

}
