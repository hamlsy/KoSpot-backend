package com.kospot.application.friend;

import com.kospot.domain.friend.adaptor.FriendAdaptor;
import com.kospot.domain.friend.entity.FriendRequest;
import com.kospot.domain.friend.service.FriendService;
import com.kospot.domain.friend.vo.FriendRequestStatus;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.friend.service.FriendCacheRedisService;
import com.kospot.presentation.friend.dto.response.FriendRequestActionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class RejectFriendRequestUseCase {

    private final FriendAdaptor friendAdaptor;
    private final FriendService friendService;
    private final FriendCacheRedisService friendCacheRedisService;

    public FriendRequestActionResponse execute(Member receiver, Long requestId) {
        FriendRequest request = friendAdaptor.queryRequestById(requestId);
        request.reject(receiver.getId());
        friendService.saveRequest(request);

        friendCacheRedisService.evictIncomingRequests(receiver.getId());

        return new FriendRequestActionResponse(request.getId(), FriendRequestStatus.REJECTED.name());
    }
}
