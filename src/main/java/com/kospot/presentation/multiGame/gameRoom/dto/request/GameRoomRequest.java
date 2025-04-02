package com.kospot.presentation.multiGame.gameRoom.dto.request;

import com.kospot.domain.game.entity.GameMode;
import com.kospot.domain.game.entity.GameType;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoomStatus;
import lombok.*;

public class GameRoomRequest {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Find {
        private String keyword;
    }

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
                    .playerMatchType(GameType.fromKey(gameTypeKey))
                    .status(GameRoomStatus.WAITING)
                    .privateRoom(privateRoom)
                    .currentPlayerCount(1)
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Update {

        private String title;
        private String password;
        private String gameModeKey;
        private String playerMatchTypeKey;
        private boolean privateRoom;
        private int teamCount;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Join {
        private String password;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Kick {
        private Long targetPlayerId;
    }
}
