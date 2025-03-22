package com.kospot.kospot.presentation.multiplay.gameRoom.dto.request;

import com.kospot.kospot.domain.game.entity.GameMode;
import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.multiplay.gameRoom.entity.GameRoom;
import com.kospot.kospot.domain.multiplay.gameRoom.entity.GameRoomStatus;
import lombok.*;

public class GameRoomRequest {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Create {

        private String title;
        private String password;
        private String gameModeKey;
        private String gameTypeKey;
        private int maxPlayers;
        private boolean privateRoom;

        public GameRoom toEntity() {
            return GameRoom.builder()
                    .title(title)
                    .password(password)
                    .maxPlayers(maxPlayers)
                    .gameMode(GameMode.fromKey(gameModeKey))
                    .gameType(GameType.fromKey(gameTypeKey))
                    .status(GameRoomStatus.WAITING)
                    .privateRoom(privateRoom)
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Join {
        private String password;
    }
}
