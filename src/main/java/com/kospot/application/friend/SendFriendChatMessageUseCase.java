package com.kospot.application.friend;

import com.kospot.domain.friend.adaptor.FriendAdaptor;
import com.kospot.domain.friend.entity.FriendChatMessage;
import com.kospot.domain.friend.entity.FriendChatRoom;
import com.kospot.domain.friend.exception.FriendErrorStatus;
import com.kospot.domain.friend.exception.FriendHandler;
import com.kospot.domain.friend.service.FriendChatService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.friend.dto.response.FriendChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class SendFriendChatMessageUseCase {

    private final FriendAdaptor friendAdaptor;
    private final FriendChatService friendChatService;

    public FriendChatMessageResponse execute(Member member, Long roomId, String content) {
        FriendChatRoom room = friendAdaptor.queryChatRoomById(roomId);
        validateParticipant(room, member.getId());

        FriendChatMessage message = FriendChatMessage.create(roomId, member.getId(), content);
        FriendChatMessage saved = friendChatService.saveMessage(message);

        room.touchLastMessageAt();
        friendChatService.saveRoom(room);

        return new FriendChatMessageResponse(
                saved.getMessageId(),
                saved.getSenderMemberId(),
                saved.getContent(),
                saved.getCreatedDate()
        );
    }

    private void validateParticipant(FriendChatRoom room, Long memberId) {
        if (!room.isParticipant(memberId)) {
            throw new FriendHandler(FriendErrorStatus.FRIEND_CHAT_ACCESS_DENIED);
        }
    }
}
