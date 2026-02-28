package com.kospot.application.friend;

import com.kospot.domain.friend.adaptor.FriendAdaptor;
import com.kospot.domain.friend.entity.FriendChatRoom;
import com.kospot.domain.friend.exception.FriendErrorStatus;
import com.kospot.domain.friend.exception.FriendHandler;
import com.kospot.domain.friend.service.FriendChatService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.redis.domain.friend.chatstream.producer.FriendChatStreamProducer;
import com.kospot.presentation.friend.dto.response.FriendChatMessageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendFriendChatMessageUseCaseTest {

    @Mock
    private FriendAdaptor friendAdaptor;
    @Mock
    private FriendChatService friendChatService;
    @Mock
    private FriendChatStreamProducer friendChatStreamProducer;
    @Mock
    private Member member;

    @InjectMocks
    private SendFriendChatMessageUseCase useCase;

    @Test
    @DisplayName("친구 채팅 메시지는 Redis Stream에 적재되고 채팅방 마지막 메시지 시간이 갱신된다")
    void enqueueAndTouchRoomOnSuccess() {
        Long memberId = 10L;
        Long roomId = 20L;
        String content = "hello";
        FriendChatRoom room = FriendChatRoom.create(memberId, 30L, "10:30");

        when(member.getId()).thenReturn(memberId);
        when(friendAdaptor.queryChatRoomById(roomId)).thenReturn(room);

        FriendChatMessageResponse response = useCase.execute(member, roomId, content);

        ArgumentCaptor<com.kospot.domain.friend.model.FriendChatStreamMessage> captor =
                ArgumentCaptor.forClass(com.kospot.domain.friend.model.FriendChatStreamMessage.class);
        verify(friendChatStreamProducer).enqueue(captor.capture());
        verify(friendChatService).saveRoom(room);

        assertNotNull(room.getLastMessageAt());
        assertNotNull(response.messageId());
        assertEquals(memberId, response.senderMemberId());
        assertEquals(content, response.content());
        assertNotNull(response.createdAt());
        assertEquals(roomId, captor.getValue().roomId());
    }

    @Test
    @DisplayName("Redis Stream 적재 실패 시 채팅방 정보는 저장되지 않는다")
    void doesNotPersistRoomWhenStreamFails() {
        Long memberId = 10L;
        Long roomId = 20L;
        FriendChatRoom room = FriendChatRoom.create(memberId, 30L, "10:30");

        when(member.getId()).thenReturn(memberId);
        when(friendAdaptor.queryChatRoomById(roomId)).thenReturn(room);
        doThrow(new FriendHandler(FriendErrorStatus.FRIEND_CHAT_STREAM_UNAVAILABLE))
                .when(friendChatStreamProducer)
                .enqueue(org.mockito.ArgumentMatchers.any());

        assertThrows(FriendHandler.class, () -> useCase.execute(member, roomId, "hello"));

        verify(friendChatService, never()).saveRoom(room);
    }
}
