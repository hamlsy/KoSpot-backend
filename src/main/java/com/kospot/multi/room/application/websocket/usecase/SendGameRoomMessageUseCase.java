package com.kospot.multi.room.application.websocket.usecase;

import com.kospot.multi.room.application.websocket.command.SendGameRoomMessageCommand;
import com.kospot.chat.domain.entity.ChatMessage;
import com.kospot.chat.application.service.ChatService;
import com.kospot.chat.domain.vo.MessageType;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.exception.object.domain.WebSocketHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.common.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.chat.presentation.dto.request.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.scheduling.annotation.Async;

@UseCase
@RequiredArgsConstructor
public class SendGameRoomMessageUseCase {

    private final ChatService chatService;
    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;

    @Async("chatRoomExecutor")
    public void execute(String roomId, ChatMessageDto.GameRoom dto, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal webSocketMemberPrincipal = WebSocketMemberPrincipal.getPrincipal(headerAccessor);
        MemberProfileRedisAdaptor.MemberProfileView memberProfileView =
                memberProfileRedisAdaptor.findProfile(webSocketMemberPrincipal.getMemberId());

        SendGameRoomMessageCommand command = SendGameRoomMessageCommand.from(roomId, dto, memberProfileView);
        validateCommand(command);
        ChatMessage chatMessage = createGameRoomChatMessage(command);
        chatService.sendGameRoomMessage(chatMessage);
    }

    private ChatMessage createGameRoomChatMessage(SendGameRoomMessageCommand command) {
        return ChatMessage.builder()
                .memberId(command.getMemberId())
                .nickname(command.getNickname())
                .messageType(MessageType.GAME_ROOM_CHAT)
                .gameRoomId(command.getGameRoomId())
                .content(command.getContent())
                .build();
    }

    private void validateCommand(SendGameRoomMessageCommand command) {
        validateContent(command);
        // todo Additional validations
    }

    private void validateContent(SendGameRoomMessageCommand command) {
        if (command.getContent() == null || command.getContent().isEmpty()) {
            throw new WebSocketHandler(ErrorStatus.CHAT_MESSAGE_CONTENT_EMPTY);
        }
    }
}
