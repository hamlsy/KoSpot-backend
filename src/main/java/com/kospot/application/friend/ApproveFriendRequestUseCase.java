package com.kospot.application.friend;

import com.kospot.domain.friend.adaptor.FriendAdaptor;
import com.kospot.domain.friend.entity.FriendRequest;
import com.kospot.domain.friend.entity.Friendship;
import com.kospot.domain.friend.service.FriendPairService;
import com.kospot.domain.friend.service.FriendService;
import com.kospot.domain.friend.vo.FriendshipStatus;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.friend.service.FriendCacheRedisService;
import com.kospot.presentation.friend.dto.response.FriendRequestActionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class ApproveFriendRequestUseCase {

    private final FriendAdaptor friendAdaptor;
    private final FriendService friendService;
    private final FriendPairService friendPairService;
    private final FriendCacheRedisService friendCacheRedisService;

    public FriendRequestActionResponse execute(Member receiver, Long requestId) {
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
