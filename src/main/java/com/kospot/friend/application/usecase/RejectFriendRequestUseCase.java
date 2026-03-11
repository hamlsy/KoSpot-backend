package com.kospot.friend.application.usecase;

import com.kospot.friend.application.adaptor.FriendAdaptor;
import com.kospot.friend.domain.entity.FriendRequest;
import com.kospot.friend.application.service.FriendService;
import com.kospot.friend.domain.vo.FriendRequestStatus;
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
public class RejectFriendRequestUseCase {

    private final MemberAdaptor memberAdaptor;
    private final FriendAdaptor friendAdaptor;
    private final FriendService friendService;
    private final FriendCacheRedisService friendCacheRedisService;

    public FriendRequestActionResponse execute(Long receiverId, Long requestId) {
        Member receiver = memberAdaptor.queryById(receiverId);
        FriendRequest request = friendAdaptor.queryRequestById(requestId);
        request.reject(receiver.getId());
        friendService.saveRequest(request);

        friendCacheRedisService.evictIncomingRequests(receiver.getId());

        return new FriendRequestActionResponse(request.getId(), FriendRequestStatus.REJECTED.name());
    }
}
