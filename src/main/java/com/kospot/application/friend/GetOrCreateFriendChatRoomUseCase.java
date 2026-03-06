package com.kospot.application.friend;

import com.kospot.domain.friend.adaptor.FriendAdaptor;
import com.kospot.domain.friend.entity.FriendChatRoom;
import com.kospot.domain.friend.entity.Friendship;
import com.kospot.domain.friend.exception.FriendErrorStatus;
import com.kospot.domain.friend.exception.FriendHandler;
import com.kospot.domain.friend.service.FriendChatService;
import com.kospot.domain.friend.service.FriendPairService;
import com.kospot.domain.friend.vo.FriendshipStatus;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.friend.dto.response.FriendChatRoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class GetOrCreateFriendChatRoomUseCase {

    private final MemberAdaptor memberAdaptor;
    private final FriendAdaptor friendAdaptor;
    private final FriendPairService friendPairService;
    private final FriendChatService friendChatService;

    public FriendChatRoomResponse execute(Long memberId, Long friendMemberId) {
        Member member = memberAdaptor.queryById(memberId);
        String canonicalPairKey = friendPairService.canonicalPairKey(member.getId(), friendMemberId);
        Friendship friendship = friendAdaptor.queryFriendshipByCanonicalPair(canonicalPairKey)
                .orElseThrow(() -> new FriendHandler(FriendErrorStatus.FRIENDSHIP_NOT_FOUND));

        if (friendship.getStatus() != FriendshipStatus.ACTIVE) {
            throw new FriendHandler(FriendErrorStatus.FRIENDSHIP_NOT_FOUND);
        }

        Long memberLowId = friendPairService.lowMemberId(member.getId(), friendMemberId);
        Long memberHighId = friendPairService.highMemberId(member.getId(), friendMemberId);

        FriendChatRoom room = friendAdaptor.queryChatRoomByCanonicalPair(canonicalPairKey)
                .orElseGet(() -> friendChatService.saveRoom(
                        FriendChatRoom.create(memberLowId, memberHighId, canonicalPairKey)
                ));

        return new FriendChatRoomResponse(room.getId(), friendMemberId);
    }
}
