package com.kospot.presentation.multi.room.dto.response;

import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GameRoomDetailResponse {

    private Long roomId;
    private String title;
    private int timeLimit;
    private String gameMode;
    private String gameType;
    private boolean privateRoom;
    private int maxPlayers;
    private int totalRounds;
    private boolean isPoiNameVisible;

    private List<GameRoomPlayerResponse> connectedPlayers;

    public static GameRoomDetailResponse from(GameRoom gameRoom,
                                              List<GameRoomPlayerInfo> playerInfos) {
        return GameRoomDetailResponse.builder()
                .roomId(gameRoom.getId())
                .title(gameRoom.getTitle())
                .timeLimit(gameRoom.getTimeLimit())
                .gameMode(gameRoom.getGameMode().name())
                .gameType(gameRoom.getPlayerMatchType().name())
                .maxPlayers(gameRoom.getMaxPlayers())
                .privateRoom(gameRoom.isPrivateRoom())
                .connectedPlayers(
                        playerInfos.stream().map(
                                p -> p.toResponse(p))
                                .collect(Collectors.toList())
                )
                .totalRounds(gameRoom.getTotalRounds())
                .isPoiNameVisible(gameRoom.isPoiNameVisible())
                .build();
    }

}
