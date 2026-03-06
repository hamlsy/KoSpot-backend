package com.kospot.domain.friend.repository;

import com.kospot.gamerank.domain.vo.RankLevel;
import com.kospot.gamerank.domain.vo.RankTier;

public record FriendSummaryQueryModel(
        Long friendMemberId,
        String nickname,
        String equippedMarkerImageUrl,
        RankTier roadViewRankTier,
        RankLevel roadViewRankLevel,
        int roadViewRatingScore
) {
}
