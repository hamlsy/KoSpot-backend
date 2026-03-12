package com.kospot.friend.application.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kospot.friend.application.adaptor.FriendAdaptor;
import com.kospot.friend.infrastructure.persistence.FriendSummaryQueryModel;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.friend.infrastructure.redis.service.FriendCacheRedisService;
import com.kospot.friend.infrastructure.redis.service.FriendOnlineStatusService;
import com.kospot.friend.presentation.dto.response.FriendListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMyFriendsUseCase {

    private final MemberAdaptor memberAdaptor;
    private final FriendAdaptor friendAdaptor;
    private final FriendCacheRedisService friendCacheRedisService;
    private final FriendOnlineStatusService friendOnlineStatusService;

    public List<FriendListResponse> execute(Long memberId) {
        memberAdaptor.queryById(memberId);

        List<FriendListResponse> cachedOrFresh = friendCacheRedisService
                .getFriendList(memberId, new TypeReference<List<FriendListResponse>>() {})
                .orElseGet(() -> {
                    List<FriendSummaryQueryModel> summaries = friendAdaptor.queryFriendSummaries(memberId);

                    List<FriendListResponse> response = summaries.stream()
                            .map(summary -> FriendListResponse.builder()
                                    .friendMemberId(summary.friendMemberId())
                                    .nickname(summary.nickname())
                                    .equippedMarkerImageUrl(summary.equippedMarkerImageUrl())
                                    .online(false)
                                    .roadViewRankTier(summary.roadViewRankTier())
                                    .roadViewRankLevel(summary.roadViewRankLevel())
                                    .roadViewRatingScore(summary.roadViewRatingScore())
                                    .build())
                            .toList();

                    friendCacheRedisService.setFriendList(memberId, response);
                    return response;
                });

        Set<Long> onlineMemberIds = friendOnlineStatusService.getOnlineMemberIds(
                cachedOrFresh.stream().map(FriendListResponse::friendMemberId).toList()
        );

        return cachedOrFresh.stream()
                .map(friend -> FriendListResponse.builder()
                        .friendMemberId(friend.friendMemberId())
                        .nickname(friend.nickname())
                        .equippedMarkerImageUrl(friend.equippedMarkerImageUrl())
                        .online(onlineMemberIds.contains(friend.friendMemberId()))
                        .roadViewRankTier(friend.roadViewRankTier())
                        .roadViewRankLevel(friend.roadViewRankLevel())
                        .roadViewRatingScore(friend.roadViewRatingScore())
                        .build())
                .toList();
    }
}
