package com.kospot.multi.lobby.application.usecase;

import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.multi.lobby.infrastructure.websocket.service.LobbyPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class JoinGlobalLobbyUseCase {

    private final LobbyPresenceService lobbyPresenceService;

    public long execute(SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal webSocketMemberPrincipal = WebSocketMemberPrincipal.getPrincipal(headerAccessor);
        lobbyPresenceService.joinGlobalLobby(webSocketMemberPrincipal.getMemberId(), headerAccessor.getSessionId());
        return lobbyPresenceService.getLobbyUserCount();
    }

}
