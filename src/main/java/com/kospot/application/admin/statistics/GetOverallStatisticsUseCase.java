package com.kospot.application.admin.statistics;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.statistic.adaptor.GameModeStatisticAdaptor;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.admin.dto.response.GameModeStatisticSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetOverallStatisticsUseCase {

    private final GameModeStatisticAdaptor gameModeStatisticAdaptor;

    public List<GameModeStatisticSummary> execute() {
        List<Object[]> results = gameModeStatisticAdaptor.queryOverallStatisticsByMode();
        
        return results.stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    private GameModeStatisticSummary mapToSummary(Object[] row) {
        GameMode gameMode = (GameMode) row[0];
        Long totalGames = ((Number) row[1]).longValue();
        Double avgPracticeScore = row[2] != null ? ((Number) row[2]).doubleValue() : null;
        Double avgRankScore = row[3] != null ? ((Number) row[3]).doubleValue() : null;
        Double avgMultiScore = row[4] != null ? ((Number) row[4]).doubleValue() : null;
        Long totalFirstPlace = row[5] != null ? ((Number) row[5]).longValue() : 0L;
        Long totalSecondPlace = row[6] != null ? ((Number) row[6]).longValue() : 0L;
        Long totalThirdPlace = row[7] != null ? ((Number) row[7]).longValue() : 0L;

        return new GameModeStatisticSummary(
                gameMode,
                totalGames,
                avgPracticeScore,
                avgRankScore,
                avgMultiScore,
                totalFirstPlace,
                totalSecondPlace,
                totalThirdPlace
        );
    }
}

