package com.kospot.multi.lobby.application.usecase;

import com.kospot.multi.lobby.application.command.SendGlobalLobbyMessageCommand;
import com.kospot.chat.domain.entity.ChatMessage;
import com.kospot.chat.application.service.ChatService;
import com.kospot.chat.domain.vo.MessageType;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.exception.object.domain.WebSocketHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.member.infrastructure.redis.service.MemberProfileRedisService;
import com.kospot.common.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.chat.presentation.dto.request.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.scheduling.annotation.Async;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class SendGlobalLobbyMessageUseCase {

    private final MemberAdaptor memberAdaptor;
    private final ChatService chatService;

    //redis
    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;
    private final MemberProfileRedisService memberProfileRedisService;

    @Async("chatRoomExecutor")
    public void execute(ChatMessageDto.Lobby dto, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal webSocketMemberPrincipal = (WebSocketMemberPrincipal) headerAccessor.getSessionAttributes().get("user");
        Long memberId = webSocketMemberPrincipal.getMemberId();

        // redis 조회
        if(memberProfileRedisAdaptor.findProfile(webSocketMemberPrincipal.getMemberId()) == null) {
            Member member = memberAdaptor.queryByIdFetchMarkerImage(memberId);
            memberProfileRedisService.saveProfile(memberId, member.getNickname(), member.getEquippedMarkerImage().getImageUrl());
        }

        MemberProfileRedisAdaptor.MemberProfileView profileView = memberProfileRedisAdaptor.findProfile(memberId);

        SendGlobalLobbyMessageCommand command = SendGlobalLobbyMessageCommand.from(dto, profileView);
        validateCommand(command);
        ChatMessage chatMessage = createChatMessage(command);
        chatService.sendGlobalLobbyMessage(chatMessage);
    }

    private ChatMessage createChatMessage(SendGlobalLobbyMessageCommand command) {
        return ChatMessage.builder()
                .memberId(command.getMemberId())
                .nickname(command.getNickname())
                .messageType(MessageType.GLOBAL_CHAT)
                .content(command.getContent())
                .build();
    }

    private void validateCommand(SendGlobalLobbyMessageCommand command) {
        validateContent(command);
        // todo Additional validations
    }

    private void validateContent(SendGlobalLobbyMessageCommand command) {
        if (command.getContent() == null || command.getContent().isEmpty()) {
            throw new WebSocketHandler(ErrorStatus.CHAT_MESSAGE_CONTENT_EMPTY);
        }
    }

}
