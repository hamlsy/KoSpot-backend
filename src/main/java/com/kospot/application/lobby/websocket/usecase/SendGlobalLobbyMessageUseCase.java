package com.kospot.application.lobby.websocket.usecase;

import com.kospot.application.lobby.websocket.command.SendGlobalLobbyMessageCommand;
import com.kospot.domain.chat.entity.ChatMessage;
import com.kospot.domain.chat.service.ChatService;
import com.kospot.domain.chat.vo.ChannelType;
import com.kospot.domain.chat.vo.MessageType;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.exception.object.domain.WebSocketHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.redis.domain.member.adaptor.MemberProfileRedisAdaptor;
import com.kospot.infrastructure.redis.domain.member.service.MemberProfileRedisService;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
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
