package com.kospot.application.friend;

import com.kospot.domain.friend.exception.FriendErrorStatus;
import com.kospot.domain.friend.exception.FriendHandler;
import com.kospot.domain.friend.model.FriendChatStreamMessage;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.friend.chatstream.producer.FriendChatStreamProducer;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.domain.friend.constants.FriendChatChannelConstants;
import com.kospot.infrastructure.websocket.domain.friend.service.FriendChatSubscriptionCacheService;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import com.kospot.presentation.friend.dto.response.FriendChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;

@UseCase
@RequiredArgsConstructor
public class SendFriendChatMessageUseCase {

    private final FriendChatSubscriptionCacheService friendChatSubscriptionCacheService;
    private final FriendChatStreamProducer friendChatStreamProducer;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public FriendChatMessageResponse execute(Long roomId, ChatMessageDto.Friend dto, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal principal = resolvePrincipal(headerAccessor);
        validateSendPermission(headerAccessor.getSessionId(), roomId);
        validateContent(dto.getContent());

        FriendChatStreamMessage message = FriendChatStreamMessage.create(roomId, principal.getMemberId(), dto.getContent().trim());
        friendChatStreamProducer.enqueue(message);

        FriendChatMessageResponse response = new FriendChatMessageResponse(
                message.messageId(),
                message.senderMemberId(),
                message.content(),
                message.createdAt()
        );

        simpMessagingTemplate.convertAndSend(FriendChatChannelConstants.getFriendChatRoomChannel(roomId), response);
        return response;
    }

    private void validateSendPermission(String sessionId, Long roomId) {
        boolean allowed = friendChatSubscriptionCacheService.isAllowed(sessionId, roomId);
        if (!allowed) {
            throw new FriendHandler(FriendErrorStatus.FRIEND_CHAT_ACCESS_DENIED);
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank() || content.length() > 500) {
            throw new FriendHandler(FriendErrorStatus.FRIEND_CHAT_ACCESS_DENIED);
        }
    }

    private WebSocketMemberPrincipal resolvePrincipal(SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal instanceof WebSocketMemberPrincipal wsPrincipal) {
            return wsPrincipal;
        }

        WebSocketMemberPrincipal sessionPrincipal = WebSocketMemberPrincipal.getPrincipal(headerAccessor);
        if (sessionPrincipal != null) {
            return sessionPrincipal;
        }

        throw new FriendHandler(FriendErrorStatus.FRIEND_CHAT_ACCESS_DENIED);
    }
}
