package com.kospot.friend.application.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kospot.friend.application.adaptor.FriendAdaptor;
import com.kospot.friend.infrastructure.persistence.FriendSummaryQueryModel;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.friend.infrastructure.redis.service.FriendCacheRedisService;
import com.kospot.multi.lobby.infrastructure.websocket.service.LobbyPresenceService;
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
    private final LobbyPresenceService lobbyPresenceService;

    public List<FriendListResponse> execute(Long memberId) {
        Member member = memberAdaptor.queryById(memberId);

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
