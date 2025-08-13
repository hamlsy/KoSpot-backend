package com.kospot.application.multiplayer.gameroom.websocket.usecase;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.exception.object.domain.GameRoomHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class JoinGameRoomSocketUseCase {

    private final MemberAdaptor memberAdaptor;
    private final GameRoomRedisService gameRoomRedisService;

    public void execute(String roomId, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal webSocketMemberPrincipal = (WebSocketMemberPrincipal) headerAccessor.getSessionAttributes().get("user");
        validatePlayerInRoom(roomId, webSocketMemberPrincipal.getMemberId());
    }

    public void validatePlayerInRoom(String roomId, Long memberId) {
        List<GameRoomPlayerInfo> gameRoomPlayerInfos = gameRoomRedisService.getRoomPlayers(roomId);
        boolean isPlayerInRoom = gameRoomPlayerInfos.stream()
                .anyMatch(p -> p.getMemberId().equals(memberId));

        if (!isPlayerInRoom) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_PLAYER_NOT_FOUND);
        }
    }

}
