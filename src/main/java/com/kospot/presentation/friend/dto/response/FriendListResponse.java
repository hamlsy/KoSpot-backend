package com.kospot.presentation.friend.dto.response;

import com.kospot.gamerank.domain.vo.RankLevel;
import com.kospot.gamerank.domain.vo.RankTier;
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
