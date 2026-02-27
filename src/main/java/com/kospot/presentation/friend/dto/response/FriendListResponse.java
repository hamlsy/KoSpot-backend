package com.kospot.presentation.friend.dto.response;

import com.kospot.domain.gamerank.vo.RankLevel;
import com.kospot.domain.gamerank.vo.RankTier;
import lombok.Builder;

@Builder
public record FriendListResponse(
        Long friendMemberId,
        String nickname,
        String equippedMarkerImageUrl,
        boolean online,
        RankTier roadViewRankTier,
        RankLevel roadViewRankLevel,
        int roadViewRatingScore
) {
}
