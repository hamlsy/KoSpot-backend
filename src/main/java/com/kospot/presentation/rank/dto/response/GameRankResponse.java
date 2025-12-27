package com.kospot.presentation.rank.dto.response;

import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.gamerank.vo.RankLevel;
import com.kospot.domain.gamerank.vo.RankTier;
import com.kospot.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

public class GameRankResponse {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Ranking {
        private MyRankInfo myRank;
        private List<PlayerSummary> players;
    }

    @Data
    @Builder
    public static class MyRankInfo {
        private String nickname;
        private RankTier rankTier;
        private RankLevel rankLevel;
        private int ratingScore;

        public static MyRankInfo from(Member member, GameRank gameRank) {
            return MyRankInfo.builder()
                    .nickname(member.getNickname())
                    .rankTier(gameRank.getRankTier())
                    .rankLevel(gameRank.getRankLevel())
                    .ratingScore(gameRank.getRatingScore())
                    .build();
        }
    }

    @Data
    @Builder
    public static class PlayerSummary {
        private Long memberId;
        private String nickname;
        private RankTier rankTier;
        private RankLevel rankLevel;
        private int ratingScore;

        public static PlayerSummary from(Member member, GameRank gameRank) {
            return PlayerSummary.builder()
                    .memberId(member.getId())
                    .nickname(member.getNickname())
                    .rankTier(gameRank.getRankTier())
                    .rankLevel(gameRank.getRankLevel())
                    .ratingScore(gameRank.getRatingScore())
                    .build();
        }
    }
}
