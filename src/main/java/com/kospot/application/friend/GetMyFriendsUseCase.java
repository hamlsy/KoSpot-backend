package com.kospot.application.friend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kospot.domain.friend.adaptor.FriendAdaptor;
import com.kospot.domain.friend.repository.FriendSummaryQueryModel;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.friend.service.FriendCacheRedisService;
import com.kospot.infrastructure.websocket.domain.multi.lobby.service.LobbyPresenceService;
import com.kospot.presentation.friend.dto.response.FriendListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMyFriendsUseCase {

    private final FriendAdaptor friendAdaptor;
    private final FriendCacheRedisService friendCacheRedisService;
    private final LobbyPresenceService lobbyPresenceService;

    public List<FriendListResponse> execute(Member member) {
        Long memberId = member.getId();

        return friendCacheRedisService
                .getFriendList(memberId, new TypeReference<List<FriendListResponse>>() {})
                .orElseGet(() -> {
                    List<FriendSummaryQueryModel> summaries = friendAdaptor.queryFriendSummaries(memberId);
                    Set<Long> onlineMemberIds = lobbyPresenceService.getOnlineMemberIds();

                    List<FriendListResponse> response = summaries.stream()
                            .map(summary -> FriendListResponse.builder()
                                    .friendMemberId(summary.friendMemberId())
                                    .nickname(summary.nickname())
                                    .equippedMarkerImageUrl(summary.equippedMarkerImageUrl())
                                    .online(onlineMemberIds.contains(summary.friendMemberId()))
                                    .roadViewRankTier(summary.roadViewRankTier())
                                    .roadViewRankLevel(summary.roadViewRankLevel())
                                    .roadViewRatingScore(summary.roadViewRatingScore())
                                    .build())
                            .toList();

                    friendCacheRedisService.setFriendList(memberId, response);
                    return response;
                });
    }
}
