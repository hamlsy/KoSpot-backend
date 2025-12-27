package com.kospot.presentation.rank.dto.response;

import com.kospot.domain.gamerank.vo.RankLevel;
import com.kospot.domain.gamerank.vo.RankTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

public class GameRankResponse {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Ranking {
        Page<Player> players;
    }

    @Data
    @Builder
    public static class Player {
        private Long memberId;
        private String nickname;
        private String equippedMarkerImageUrl;
        private RankTier rankTier;
        private RankLevel rankLevel;
        private int ratingScore;
    }
}
