package com.kospot.application.friend;

import com.kospot.domain.friend.adaptor.FriendAdaptor;
import com.kospot.domain.friend.entity.FriendChatRoom;
import com.kospot.domain.friend.exception.FriendErrorStatus;
import com.kospot.domain.friend.exception.FriendHandler;
import com.kospot.domain.friend.model.FriendChatStreamMessage;
import com.kospot.domain.friend.service.FriendChatService;
import com.kospot.infrastructure.redis.domain.friend.chatstream.producer.FriendChatStreamProducer;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.friend.dto.response.FriendChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class SendFriendChatMessageUseCase {

    private final MemberAdaptor memberAdaptor;
    private final FriendAdaptor friendAdaptor;
    private final FriendChatService friendChatService;
    private final FriendChatStreamProducer friendChatStreamProducer;

    public FriendChatMessageResponse execute(Long memberId, Long roomId, String content) {
        Member member = memberAdaptor.queryById(memberId);
        FriendChatRoom room = friendAdaptor.queryChatRoomById(roomId);
        validateParticipant(room, member.getId());

        FriendChatStreamMessage message = FriendChatStreamMessage.create(roomId, member.getId(), content);
        friendChatStreamProducer.enqueue(message);

        room.touchLastMessageAt();
        friendChatService.saveRoom(room);

        return new FriendChatMessageResponse(
                message.messageId(),
                message.senderMemberId(),
                message.content(),
                message.createdAt()
        );
    }

    private void validateParticipant(FriendChatRoom room, Long memberId) {
        if (!room.isParticipant(memberId)) {
            throw new FriendHandler(FriendErrorStatus.FRIEND_CHAT_ACCESS_DENIED);
        }
    }
}
