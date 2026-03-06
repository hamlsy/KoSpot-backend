package com.kospot.friend.application.usecase;

import com.kospot.friend.application.adaptor.FriendAdaptor;
import com.kospot.friend.domain.entity.FriendChatRoom;
import com.kospot.friend.domain.exception.FriendErrorStatus;
import com.kospot.friend.domain.exception.FriendHandler;
import com.kospot.friend.application.service.FriendChatService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.friend.presentation.dto.response.FriendChatMessageResponse;
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
