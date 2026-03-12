package com.kospot.application.friend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kospot.friend.application.adaptor.FriendAdaptor;
import com.kospot.friend.application.usecase.GetMyFriendsUseCase;
import com.kospot.friend.infrastructure.persistence.FriendSummaryQueryModel;
import com.kospot.friend.infrastructure.redis.service.FriendCacheRedisService;
import com.kospot.friend.infrastructure.redis.service.FriendOnlineStatusService;
import com.kospot.friend.presentation.dto.response.FriendListResponse;
import com.kospot.gamerank.domain.vo.RankLevel;
import com.kospot.gamerank.domain.vo.RankTier;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMyFriendsUseCaseTest {

    @Mock
    private MemberAdaptor memberAdaptor;
    @Mock
    private FriendAdaptor friendAdaptor;
    @Mock
    private FriendCacheRedisService friendCacheRedisService;
    @Mock
    private FriendOnlineStatusService friendOnlineStatusService;

    @InjectMocks
    private GetMyFriendsUseCase useCase;

    @Test
    @DisplayName("캐시 hit여도 online 값은 실시간 상태로 overwrite 된다")
    void overwriteOnlineWhenCacheHit() {
        Long memberId = 100L;
        when(memberAdaptor.queryById(memberId)).thenReturn(mock(Member.class));

        List<FriendListResponse> cached = List.of(
                FriendListResponse.builder()
                        .friendMemberId(1L)
                        .nickname("a")
                        .equippedMarkerImageUrl("img-a")
                        .online(true)
                        .roadViewRankTier(RankTier.GOLD)
                        .roadViewRankLevel(RankLevel.THREE)
                        .roadViewRatingScore(1300)
                        .build(),
                FriendListResponse.builder()
                        .friendMemberId(2L)
                        .nickname("b")
                        .equippedMarkerImageUrl("img-b")
                        .online(false)
                        .roadViewRankTier(RankTier.SILVER)
                        .roadViewRankLevel(RankLevel.TWO)
                        .roadViewRatingScore(1100)
                        .build()
        );

        when(friendCacheRedisService.getFriendList(eq(memberId), any(TypeReference.class)))
                .thenReturn(Optional.of(cached));
        when(friendOnlineStatusService.getOnlineMemberIds(List.of(1L, 2L)))
                .thenReturn(Set.of(2L));

        List<FriendListResponse> result = useCase.execute(memberId);

        assertFalse(result.get(0).online());
        assertTrue(result.get(1).online());
        verify(friendAdaptor, never()).queryFriendSummaries(any());
    }

    @Test
    @DisplayName("캐시 miss면 기본 목록을 캐시하고 online은 별도 계산해 반환한다")
    void cacheStaticDataAndComputeOnlineWhenCacheMiss() {
        Long memberId = 200L;
        when(memberAdaptor.queryById(memberId)).thenReturn(mock(Member.class));
        when(friendCacheRedisService.getFriendList(eq(memberId), any(TypeReference.class)))
                .thenReturn(Optional.empty());

        List<FriendSummaryQueryModel> summaries = List.of(
                new FriendSummaryQueryModel(10L, "u10", "img10", RankTier.BRONZE, RankLevel.ONE, 900),
                new FriendSummaryQueryModel(20L, "u20", "img20", RankTier.PLATINUM, RankLevel.FOUR, 3400)
        );
        when(friendAdaptor.queryFriendSummaries(memberId)).thenReturn(summaries);
        when(friendOnlineStatusService.getOnlineMemberIds(List.of(10L, 20L))).thenReturn(Set.of(20L));

        List<FriendListResponse> result = useCase.execute(memberId);

        assertEquals(2, result.size());
        assertFalse(result.get(0).online());
        assertTrue(result.get(1).online());

        ArgumentCaptor<List<FriendListResponse>> cacheCaptor = ArgumentCaptor.forClass(List.class);
        verify(friendCacheRedisService).setFriendList(eq(memberId), cacheCaptor.capture());
        List<FriendListResponse> cachedPayload = cacheCaptor.getValue();
        assertFalse(cachedPayload.get(0).online());
        assertFalse(cachedPayload.get(1).online());
    }
}
