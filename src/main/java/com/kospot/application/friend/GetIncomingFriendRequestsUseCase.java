package com.kospot.application.friend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kospot.domain.friend.adaptor.FriendAdaptor;
import com.kospot.domain.friend.entity.FriendRequest;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.friend.service.FriendCacheRedisService;
import com.kospot.infrastructure.redis.domain.member.adaptor.MemberProfileRedisAdaptor;
import com.kospot.presentation.friend.dto.response.IncomingFriendRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetIncomingFriendRequestsUseCase {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final MemberAdaptor memberAdaptor;
    private final FriendAdaptor friendAdaptor;
    private final FriendCacheRedisService friendCacheRedisService;
    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;

    public List<IncomingFriendRequestResponse> execute(Long memberId, int page, Integer size) {
        Member member = memberAdaptor.queryById(memberId);
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(size, 1), 50);

        if (page == 0) {
            return friendCacheRedisService
                    .getIncomingRequests(memberId, new TypeReference<List<IncomingFriendRequestResponse>>() {})
                    .orElseGet(() -> {
                        List<IncomingFriendRequestResponse> result = queryIncoming(memberId, 0, pageSize);
                        friendCacheRedisService.setIncomingRequests(memberId, result);
                        return result;
                    });
        }

        return queryIncoming(memberId, page, pageSize);
    }

    private List<IncomingFriendRequestResponse> queryIncoming(Long memberId, int page, int size) {
        List<FriendRequest> requests = friendAdaptor.queryIncomingPendingRequests(memberId, page, size);

        return requests.stream()
                .map(request -> {
                    MemberProfileRedisAdaptor.MemberProfileView senderProfile =
                            memberProfileRedisAdaptor.findProfile(request.getRequesterMemberId());
                    return new IncomingFriendRequestResponse(
                            request.getId(),
                            request.getRequesterMemberId(),
                            senderProfile.nickname(),
                            senderProfile.markerImageUrl(),
                            request.getCreatedDate()
                    );
                })
                .toList();
    }
}
