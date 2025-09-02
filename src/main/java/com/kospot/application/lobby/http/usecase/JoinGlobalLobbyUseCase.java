package com.kospot.application.lobby.http.usecase;

import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.domain.lobby.service.LobbyPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class JoinGlobalLobbyUseCase {

    private final LobbyPresenceService lobbyPresenceService;

    public void execute(SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal webSocketMemberPrincipal = (WebSocketMemberPrincipal) headerAccessor.getSessionAttributes().get("user");
        lobbyPresenceService.joinGlobalLobby(webSocketMemberPrincipal.getMemberId(), headerAccessor.getSessionId());
    }

}
