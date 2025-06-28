package com.kospot.presentation.chat.controller;

import com.kospot.application.chat.usecase.JoinGlobalLobbyUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.chat.service.ChatService;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.infrastructure.websocket.auth.ChatMemberPrincipal;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Chat Websocket", description = "채팅 소켓")
public class ChatController {

    private final ChatService chatService;
    private final JoinGlobalLobbyUseCase joinGlobalLobbyUseCase;

    //global lobby chat
    @MessageMapping("/chat.global")
    public void sendGlobalMessage(@Payload MessageDto messageDto, @CurrentMember Member member) {
        try {
            // 메시지 유효성 검증
            if (messageDto.getContent() == null || messageDto.getContent().trim().isEmpty()) {
                return;
            }

            // 욕설 필터링 적용
            String filteredContent = chatService.filterProfanity(messageDto.getContent());

            // 글로벌 채팅 메시지 처리 (Member 정보 전달)
            chatService.processGlobalChatMessage(member, filteredContent);

        } catch (Exception e) {
            log.error("Error processing global chat message", e);
        }
    }

    @MessageMapping("/chat.join.lobby")
    public void joinGlobalLobby(SimpMessageHeaderAccessor headerAccessor) {
        ChatMemberPrincipal chatMemberPrincipal = (ChatMemberPrincipal) headerAccessor.getUser();
        joinGlobalLobbyUseCase.execute(headerAccessor);
    }

    // 글로벌 로비 퇴장
    @MessageMapping("/chat.leave.lobby")
    public void leaveGlobalLobby(SimpMessageHeaderAccessor headerAccessor) {
        chatService.leaveGlobalLobby(member.getId(), headerAccessor.getSessionId());
    }
}
