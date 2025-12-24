package com.kospot.presentation.multi.room.dto.request;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
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
        private int totalRounds;
        private int timeLimit;
        private String password;
        private String gameModeKey;
        private String playerMatchTypeKey; //individual or team
        private int maxPlayers;
        private boolean isPoiNameVisible;
        private boolean privateRoom;

        public GameRoom toEntity() {
            return GameRoom.builder()
                    .title(title)
                    .password(password)
                    .timeLimit(timeLimit)
                    .maxPlayers(maxPlayers)
                    .gameMode(GameMode.fromKey(gameModeKey))
                    .playerMatchType(PlayerMatchType.fromKey(playerMatchTypeKey))
                    .isPoiNameVisible(isPoiNameVisible)
                    .status(GameRoomStatus.WAITING)
                    .privateRoom(privateRoom)
                    .totalRounds(totalRounds)
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
        private int timeLimit;
        private String password;
        private String gameModeKey;
        private String playerMatchTypeKey;
        private boolean privateRoom;
        private boolean isPoiNameVisible;
        private int teamCount;
        private int totalRounds;

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


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class SwitchTeam {
        private String team;
    }
}
