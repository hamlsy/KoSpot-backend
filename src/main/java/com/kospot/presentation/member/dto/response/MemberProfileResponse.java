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
    private int currentPoint;
    private LocalDateTime joinedAt;
    private LocalDateTime lastPlayedAt;
    private int currentStreak;
    
    private GameStatistics statistics;
    private RankInfo rankInfo;
    private ItemInfo itemInfo;

    @Getter
    @Builder
    public static class GameStatistics {
        private SingleGameStats singleGame;
        private MultiGameStats multiGame;
        private double bestScore;
        
        @Getter
        @Builder
        public static class SingleGameStats {
            private GameModeStats practice;
            private GameModeStats rank;
            
            @Getter
            @Builder
            public static class GameModeStats {
                private long totalGames;
                private double averageScore;
            }
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
        
        @Getter
        @Builder
        public static class RoadViewRank {
            private RankTier tier;
            private RankLevel level;
            private int ratingScore;
        }
    }

    @Getter
    @Builder
    public static class ItemInfo {
        private int totalItems;
        private int equippedItems;
    }
}

