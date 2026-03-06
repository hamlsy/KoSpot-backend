package com.kospot.application.friend;

import com.kospot.domain.friend.adaptor.FriendAdaptor;
import com.kospot.domain.friend.entity.FriendChatRoom;
import com.kospot.domain.friend.exception.FriendErrorStatus;
import com.kospot.domain.friend.exception.FriendHandler;
import com.kospot.domain.friend.service.FriendChatService;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.friend.dto.response.FriendChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetFriendChatMessagesUseCase {

    private static final int DEFAULT_PAGE_SIZE = 30;

    private final MemberAdaptor memberAdaptor;
    private final FriendAdaptor friendAdaptor;
    private final FriendChatService friendChatService;

    public List<FriendChatMessageResponse> execute(Long memberId, Long roomId, int page, Integer size) {
        Member member = memberAdaptor.queryById(memberId);
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(size, 1), 100);

        FriendChatRoom room = friendAdaptor.queryChatRoomById(roomId);
        validateParticipant(room, member.getId());

        return friendChatService.queryRecentMessages(roomId, page, pageSize).stream()
                .map(m -> new FriendChatMessageResponse(
                        m.getMessageId(),
                        m.getSenderMemberId(),
                        m.getContent(),
                        m.getCreatedDate()
                ))
                .toList();
    }

    private void validateParticipant(FriendChatRoom room, Long memberId) {
        if (!room.isParticipant(memberId)) {
            throw new FriendHandler(FriendErrorStatus.FRIEND_CHAT_ACCESS_DENIED);
        }
    }
}
