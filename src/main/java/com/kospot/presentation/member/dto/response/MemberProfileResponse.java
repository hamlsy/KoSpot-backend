package com.kospot.presentation.member.dto.response;

import com.kospot.domain.gamerank.vo.RankLevel;
import com.kospot.domain.gamerank.vo.RankTier;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberProfileResponse {
    private String nickname;
    private String email;
    private String profileImageUrl;
    private int currentPoint;
    private LocalDateTime joinedAt;
    private LocalDateTime lastPlayedAt;
    private int currentStreak;
    
    private GameStatistics statistics;
    private RankInfo rankInfo;

    @Getter
    @Builder
    public static class GameStatistics {
        private RoadViewGameStats roadView;
        private PhotoGameStats photo;
        private double bestScore;
        
        @Getter
        @Builder
        public static class RoadViewGameStats {
            private GameModeStats practice;
            private GameModeStats rank;
            private MultiGameStats multi;
        }
        
        @Getter
        @Builder
        public static class PhotoGameStats {
            private GameModeStats practice;
            private GameModeStats rank;
            private MultiGameStats multi;
        }
        
        @Getter
        @Builder
        public static class GameModeStats {
            private long totalGames;
            private double averageScore;
        }
        
        @Getter
        @Builder
        public static class MultiGameStats {
            private long totalGames;
            private double averageScore;
            private long firstPlaceCount;
            private long secondPlaceCount;
            private long thirdPlaceCount;
        }
    }

    @Getter
    @Builder
    public static class RankInfo {
        private RoadViewRank roadViewRank;
        private PhotoRank photoRank;
        
        @Getter
        @Builder
        public static class RoadViewRank {
            private RankTier tier;
            private RankLevel level;
            private int ratingScore;
        }
        
        @Getter
        @Builder
        public static class PhotoRank {
            private RankTier tier;
            private RankLevel level;
            private int ratingScore;
        }
    }

}


