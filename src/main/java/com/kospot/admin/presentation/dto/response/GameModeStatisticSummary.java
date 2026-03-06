package com.kospot.admin.presentation.dto.response;

import com.kospot.game.domain.vo.GameMode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameModeStatisticSummary {
    private GameMode gameMode;
    private Long totalGames;
    private Double avgPracticeScore;
    private Double avgRankScore;
    private Double avgMultiScore;
    private Long totalFirstPlace;

    public Double getOverallAvgScore() {
        if (avgPracticeScore == null || avgRankScore == null || avgMultiScore == null) {
            return 0.0;
        }
        return (avgPracticeScore + avgRankScore + avgMultiScore) / 3;
    }
}

