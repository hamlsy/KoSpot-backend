package com.kospot.member.presentation.response;

import com.kospot.gamerank.domain.vo.RankLevel;
import com.kospot.gamerank.domain.vo.RankTier;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PlayerSummaryResponse {
    private String nickname;
    private int playStreak;
    private String equippedMarkerImageUrl;
    private LocalDateTime joinedAt;
    
    private RankInfo rankInfo;
    private MultiGameStats multiGameStats;

    @Getter
    @Builder
    public static class RankInfo {
        private RoadViewRankInfo roadView;
        private PhotoRankInfo photo;

        @Getter
        @Builder
        public static class RoadViewRankInfo {
            private int ratingScore;
            private RankLevel rankLevel;
            private RankTier rankTier;
            private double rankAvgScore;
        }

        @Getter
        @Builder
        public static class PhotoRankInfo {
            private int ratingScore;
            private RankLevel rankLevel;
            private RankTier rankTier;
            private double rankAvgScore;
        }
    }

    @Getter
    @Builder
    public static class MultiGameStats {
        private RoadViewMultiStats roadView;
        private PhotoMultiStats photo;

        @Getter
        @Builder
        public static class RoadViewMultiStats {
            private long totalGames;
            private long firstPlaceCount;
        }

        @Getter
        @Builder
        public static class PhotoMultiStats {
            private long totalGames;
            private long firstPlaceCount;
        }
    }
}

