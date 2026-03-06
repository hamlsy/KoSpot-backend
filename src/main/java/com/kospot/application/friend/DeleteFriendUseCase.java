package com.kospot.application.friend;

import com.kospot.domain.friend.adaptor.FriendAdaptor;
import com.kospot.domain.friend.service.FriendPairService;
import com.kospot.domain.friend.service.FriendService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.friend.service.FriendCacheRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class DeleteFriendUseCase {

    private final MemberAdaptor memberAdaptor;
    private final FriendAdaptor friendAdaptor;
    private final FriendService friendService;
    private final FriendPairService friendPairService;
    private final FriendCacheRedisService friendCacheRedisService;

    public void execute(Long memberId, Long friendMemberId) {
        Member member = memberAdaptor.queryById(memberId);
        String canonicalPairKey = friendPairService.canonicalPairKey(member.getId(), friendMemberId);

        friendAdaptor.queryFriendshipByCanonicalPair(canonicalPairKey)
                .ifPresent(friendship -> {
                    friendship.delete();
                    friendService.saveFriendship(friendship);
                });

        friendCacheRedisService.evictFriendList(member.getId());
        friendCacheRedisService.evictFriendList(friendMemberId);
        friendCacheRedisService.evictIncomingRequests(member.getId());
        friendCacheRedisService.evictIncomingRequests(friendMemberId);
    }
}
