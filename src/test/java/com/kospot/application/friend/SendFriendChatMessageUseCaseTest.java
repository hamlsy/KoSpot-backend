package com.kospot.application.friend;

import com.kospot.domain.friend.exception.FriendHandler;
import com.kospot.infrastructure.redis.domain.friend.chatstream.producer.FriendChatStreamProducer;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.domain.friend.constants.FriendChatChannelConstants;
import com.kospot.infrastructure.websocket.domain.friend.service.FriendChatSubscriptionCacheService;
import com.kospot.presentation.chat.dto.request.ChatMessageDto;
import com.kospot.presentation.friend.dto.response.FriendChatMessageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendFriendChatMessageUseCaseTest {

    @Mock
    private FriendChatSubscriptionCacheService friendChatSubscriptionCacheService;
    @Mock
    private FriendChatStreamProducer friendChatStreamProducer;
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private SendFriendChatMessageUseCase useCase;

    @Test
    @DisplayName("권한이 있는 세션은 stream enqueue 후 topic fan-out 한다")
    void enqueueAndBroadcastWhenAllowed() {
        Long roomId = 20L;
        String sessionId = "s-1";

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);
        WebSocketMemberPrincipal principal = org.mockito.Mockito.mock(WebSocketMemberPrincipal.class);
        when(principal.getMemberId()).thenReturn(10L);
        headerAccessor.setUser(principal);

        ChatMessageDto.Friend dto = org.mockito.Mockito.mock(ChatMessageDto.Friend.class);
        when(dto.getContent()).thenReturn(" hello ");

        when(friendChatSubscriptionCacheService.isAllowed(sessionId, roomId)).thenReturn(true);

        FriendChatMessageResponse response = useCase.execute(roomId, dto, headerAccessor);

        assertNotNull(response.messageId());
        assertEquals(10L, response.senderMemberId());
        assertEquals("hello", response.content());
        assertNotNull(response.createdAt());

        verify(friendChatStreamProducer).enqueue(any());

        ArgumentCaptor<FriendChatMessageResponse> payloadCaptor = ArgumentCaptor.forClass(FriendChatMessageResponse.class);
        verify(simpMessagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(FriendChatChannelConstants.getFriendChatRoomChannel(roomId)),
                payloadCaptor.capture()
        );
        assertEquals(response.messageId(), payloadCaptor.getValue().messageId());
    }

    @Test
    @DisplayName("세션 권한이 없으면 전송이 차단된다")
    void denyWhenSessionNotAllowed() {
        Long roomId = 20L;
        String sessionId = "s-2";

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);
        WebSocketMemberPrincipal principal = org.mockito.Mockito.mock(WebSocketMemberPrincipal.class);
        when(principal.getMemberId()).thenReturn(10L);
        headerAccessor.setUser(principal);

        ChatMessageDto.Friend dto = org.mockito.Mockito.mock(ChatMessageDto.Friend.class);
        when(dto.getContent()).thenReturn("hello");

        when(friendChatSubscriptionCacheService.isAllowed(sessionId, roomId)).thenReturn(false);

        assertThrows(FriendHandler.class, () -> useCase.execute(roomId, dto, headerAccessor));

        verify(friendChatStreamProducer, never()).enqueue(any());
        verify(simpMessagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Object.class));
    }
}
