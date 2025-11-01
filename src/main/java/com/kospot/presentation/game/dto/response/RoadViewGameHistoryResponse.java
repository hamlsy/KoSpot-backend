package com.kospot.presentation.game.dto.response;

import com.kospot.domain.coordinate.entity.Sido;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.vo.GameType;
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
    public static class RecentThree {
        private List<GameRecord> games;

        public static RecentThree from(List<RoadViewGame> games) {
            List<GameRecord> gameRecords = games.stream()
                    .map(GameRecord::from)
                    .collect(Collectors.toList());

            return RecentThree.builder()
                    .games(gameRecords)
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

