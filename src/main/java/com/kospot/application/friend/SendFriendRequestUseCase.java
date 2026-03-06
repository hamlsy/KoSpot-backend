package com.kospot.application.friend;

import com.kospot.domain.friend.adaptor.FriendAdaptor;
import com.kospot.domain.friend.entity.FriendRequest;
import com.kospot.domain.friend.exception.FriendErrorStatus;
import com.kospot.domain.friend.exception.FriendHandler;
import com.kospot.domain.friend.service.FriendPairService;
import com.kospot.domain.friend.service.FriendService;
import com.kospot.domain.friend.vo.FriendshipStatus;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.domain.notification.event.FriendRequestCreatedEvent;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.friend.service.FriendCacheRedisService;
import com.kospot.presentation.friend.dto.response.FriendRequestActionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@UseCase
@RequiredArgsConstructor
@Transactional
public class SendFriendRequestUseCase {

    private static final long DEFAULT_REQUEST_EXPIRE_DAYS = 7L;

    private final MemberAdaptor memberAdaptor;
    private final FriendAdaptor friendAdaptor;
    private final FriendService friendService;
    private final FriendPairService friendPairService;
    private final FriendCacheRedisService friendCacheRedisService;
    private final ApplicationEventPublisher eventPublisher;

    public FriendRequestActionResponse execute(Long requesterId, Long receiverMemberId) {
        Member requester = memberAdaptor.queryById(requesterId);
        validateNotSelf(requester.getId(), receiverMemberId);
        memberAdaptor.queryById(receiverMemberId);

        String canonicalPairKey = friendPairService.canonicalPairKey(requester.getId(), receiverMemberId);

        friendAdaptor.queryFriendshipByCanonicalPair(canonicalPairKey)
                .filter(f -> f.getStatus() == FriendshipStatus.ACTIVE)
                .ifPresent(f -> {
                    throw new FriendHandler(FriendErrorStatus.ALREADY_FRIENDS);
                });

        FriendRequest request = friendAdaptor.queryRequestByCanonicalPair(canonicalPairKey)
                .map(existing -> reopenOrReturn(existing, requester.getId(), receiverMemberId))
                .orElseGet(() -> FriendRequest.create(
                        requester.getId(),
                        receiverMemberId,
                        canonicalPairKey,
                        LocalDateTime.now().plusDays(DEFAULT_REQUEST_EXPIRE_DAYS)
                ));

        FriendRequest saved = friendService.saveRequest(request);

        eventPublisher.publishEvent(new FriendRequestCreatedEvent(
                saved.getId(),
                requester.getId(),
                receiverMemberId,
                LocalDateTime.now()
        ));

        friendCacheRedisService.evictIncomingRequests(receiverMemberId);
        friendCacheRedisService.evictFriendList(requester.getId());
        friendCacheRedisService.evictFriendList(receiverMemberId);

        return new FriendRequestActionResponse(saved.getId(), saved.getStatus().name());
    }

    private void validateNotSelf(Long requesterMemberId, Long receiverMemberId) {
        if (requesterMemberId.equals(receiverMemberId)) {
            throw new FriendHandler(FriendErrorStatus.CANNOT_REQUEST_SELF);
        }
    }

    private FriendRequest reopenOrReturn(FriendRequest existing, Long requesterMemberId, Long receiverMemberId) {
        if (existing.isPending()) {
            return existing;
        }
        existing.reopen(requesterMemberId, receiverMemberId, LocalDateTime.now().plusDays(DEFAULT_REQUEST_EXPIRE_DAYS));
        return existing;
    }
}
