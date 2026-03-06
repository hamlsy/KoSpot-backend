package com.kospot.multi.room.presentation.dto.response;

import com.kospot.member.domain.entity.Member;
import com.kospot.multi.room.domain.entity.GameRoom;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
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
    private boolean host;
    private String title;
    private int timeLimit;
    private String gameMode;
    private String gameType;
    private boolean privateRoom;
    private int maxPlayers;
    private int totalRounds;
    private boolean isPoiNameVisible;

    private List<GameRoomPlayerResponse> connectedPlayers;

    public static GameRoomDetailResponse from(GameRoom gameRoom, Member member,
                                              List<GameRoomPlayerInfo> playerInfos) {
        return GameRoomDetailResponse.builder()
                .roomId(gameRoom.getId())
                .title(gameRoom.getTitle())
                .host(gameRoom.isHost(member))
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
