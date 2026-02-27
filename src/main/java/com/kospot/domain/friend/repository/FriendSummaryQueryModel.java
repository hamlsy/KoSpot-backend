package com.kospot.domain.friend.repository;

import com.kospot.domain.gamerank.vo.RankLevel;
import com.kospot.domain.gamerank.vo.RankTier;

public record FriendSummaryQueryModel(
        Long friendMemberId,
        String nickname,
        String equippedMarkerImageUrl,
        RankTier roadViewRankTier,
        RankLevel roadViewRankLevel,
        int roadViewRatingScore
) {
}
