package com.kospot.application.chat.usecase;

import com.kospot.domain.chat.service.ChatService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.auth.ChatMemberPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class JoinGlobalLobbyUseCase {

    private final ChatService chatService;

    public void execute(SimpMessageHeaderAccessor headerAccessor) {
        ChatMemberPrincipal chatMemberPrincipal = (ChatMemberPrincipal) headerAccessor.getUser();
        chatService.joinGlobalLobby(chatMemberPrincipal.getMemberId(), headerAccessor.getSessionId());
    }

}
