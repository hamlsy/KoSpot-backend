package com.kospot.domain.statistic.adaptor;

import com.kospot.domain.game.vo.GameMode;

import com.kospot.domain.statistic.entity.GameModeStatistic;
import com.kospot.domain.statistic.repository.GameModeStatisticRepository;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import com.kospot.infrastructure.exception.object.domain.StatisticHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameModeStatisticAdaptor {

    private final GameModeStatisticRepository repository;

    public GameModeStatistic queryById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new StatisticHandler(ErrorStatus.STATISTIC_NOT_FOUND));
    }

    public GameModeStatistic queryByMemberStatisticIdAndGameMode(Long memberStatisticId, GameMode gameMode) {
        return repository.findByMemberStatisticIdAndGameMode(memberStatisticId, gameMode);
    }

    public List<Object[]> queryOverallStatisticsByMode() {
        return repository.getOverallStatisticsByMode();
    }

    public List<Object[]> queryStatisticsByModeBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return repository.getStatisticsByModeBetween(startDate, endDate);
    }

    public Page<GameModeStatistic> queryTopRankers(GameMode gameMode, String matchType, Pageable pageable) {
        return repository.findTopRankers(gameMode, matchType, pageable);
    }

    public List<Object[]> queryActivePlayers(long minGames) {
        return repository.countActivePlayers(minGames);
    }

    public List<Object[]> queryScoreDistribution() {
        return repository.getScoreDistribution();
    }

    public List<Object[]> queryGameModePopularity() {
        return repository.getGameModePopularity();
    }
}

