package com.kospot.friend.application.usecase;

import com.kospot.friend.application.adaptor.FriendAdaptor;
import com.kospot.friend.domain.entity.FriendRequest;
import com.kospot.friend.domain.entity.Friendship;
import com.kospot.friend.application.service.FriendPairService;
import com.kospot.friend.application.service.FriendService;
import com.kospot.friend.domain.vo.FriendshipStatus;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.friend.infrastructure.redis.service.FriendCacheRedisService;
import com.kospot.friend.presentation.dto.response.FriendRequestActionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class ApproveFriendRequestUseCase {

    private final MemberAdaptor memberAdaptor;
    private final FriendAdaptor friendAdaptor;
    private final FriendService friendService;
    private final FriendPairService friendPairService;
    private final FriendCacheRedisService friendCacheRedisService;

    public FriendRequestActionResponse execute(Long receiverId, Long requestId) {
        Member receiver = memberAdaptor.queryById(receiverId);
        FriendRequest request = friendAdaptor.queryRequestById(requestId);
        request.approve(receiver.getId());
        friendService.saveRequest(request);

        Long requesterMemberId = request.getRequesterMemberId();
        Long receiverMemberId = request.getReceiverMemberId();
        String canonicalPairKey = friendPairService.canonicalPairKey(requesterMemberId, receiverMemberId);
        Long memberLowId = friendPairService.lowMemberId(requesterMemberId, receiverMemberId);
        Long memberHighId = friendPairService.highMemberId(requesterMemberId, receiverMemberId);

        Friendship friendship = friendAdaptor.queryFriendshipByCanonicalPair(canonicalPairKey)
                .map(existing -> {
                    existing.restore();
                    return existing;
                })
                .orElseGet(() -> Friendship.create(memberLowId, memberHighId, canonicalPairKey));

        friendService.saveFriendship(friendship);

        friendCacheRedisService.evictIncomingRequests(receiverMemberId);
        friendCacheRedisService.evictFriendList(requesterMemberId);
        friendCacheRedisService.evictFriendList(receiverMemberId);

        return new FriendRequestActionResponse(request.getId(), FriendshipStatus.ACTIVE.name());
    }
}
