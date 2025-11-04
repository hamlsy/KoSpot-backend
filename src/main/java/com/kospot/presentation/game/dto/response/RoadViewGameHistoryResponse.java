package com.kospot.presentation.game.dto.response;

import com.kospot.domain.coordinate.entity.Sido;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.vo.GameType;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.gamerank.vo.RankLevel;
import com.kospot.domain.gamerank.vo.RankTier;
import com.kospot.domain.member.entity.MemberStatistic;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class RoadViewGameHistoryResponse {

    @Getter
    @Builder
    public static class GameRecord {
        private Long gameId;
        private String poiName;
        private double answerDistance;
        private double score;
        private double answerTime;
        private LocalDateTime playedAt;
        private GameType gameType;
        private Sido practiceSido;

        public static GameRecord from(RoadViewGame game) {
            return GameRecord.builder()
                    .gameId(game.getId())
                    .poiName(game.getPoiName())
                    .answerDistance(game.getAnswerDistance())
                    .score(game.getScore())
                    .answerTime(game.getAnswerTime())
                    .playedAt(game.getCreatedDate())
                    .gameType(game.getGameType())
                    .practiceSido(game.getPracticeSido())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class RankInfo {
        private RankTier rankTier;
        private RankLevel rankLevel;
        private int ratingScore;
        private double rankPercentage;

        public static RankInfo from(GameRank gameRank, double rankPercentage) {
            return RankInfo.builder()
                    .rankTier(gameRank.getRankTier())
                    .rankLevel(gameRank.getRankLevel())
                    .ratingScore(gameRank.getRatingScore())
                    .rankPercentage(rankPercentage)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class StatisticInfo {
        private long totalPlayCount;
        private double bestScore;

        public static StatisticInfo from(MemberStatistic statistic) {
            long totalPlayCount = statistic.getRoadviewRankGames() + statistic.getRoadviewPracticeGames();
            return StatisticInfo.builder()
                    .totalPlayCount(totalPlayCount)
                    .bestScore(statistic.getBestScore())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class RecentThree {
        private RankInfo rankInfo;
        private StatisticInfo statisticInfo;
        private List<GameRecord> recentGames;

        public static RecentThree from(
                GameRank gameRank,
                double rankPercentage,
                MemberStatistic statistic,
                List<RoadViewGame> games
        ) {
            List<GameRecord> gameRecords = games.stream()
                    .map(GameRecord::from)
                    .collect(Collectors.toList());

            return RecentThree.builder()
                    .rankInfo(RankInfo.from(gameRank, rankPercentage))
                    .statisticInfo(StatisticInfo.from(statistic))
                    .recentGames(gameRecords)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class All {
        private List<GameRecord> games;
        private int currentPage;
        private int totalPages;
        private long totalElements;
        private int size;

        public static All from(org.springframework.data.domain.Page<RoadViewGame> page) {
            List<GameRecord> gameRecords = page.getContent().stream()
                    .map(GameRecord::from)
                    .collect(Collectors.toList());

            return All.builder()
                    .games(gameRecords)
                    .currentPage(page.getNumber())
                    .totalPages(page.getTotalPages())
                    .totalElements(page.getTotalElements())
                    .size(page.getSize())
                    .build();
        }
    }
}

