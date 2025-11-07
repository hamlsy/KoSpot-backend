package com.kospot.application.admin.statistics;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.statistic.repository.GameModeStatisticRepository;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.admin.dto.response.GameModeStatisticSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetStatisticsByPeriodUseCase {

    private final GameModeStatisticRepository gameModeStatisticRepository;

    public List<GameModeStatisticSummary> execute(String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = switch (period) {
            case "daily" -> now.minusDays(1);
            case "weekly" -> now.minusWeeks(1);
            case "monthly" -> now.minusMonths(1);
            default -> throw new IllegalArgumentException("Invalid period: " + period);
        };

        List<Object[]> results = gameModeStatisticRepository.getStatisticsByModeBetween(startDate, now);
        
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

