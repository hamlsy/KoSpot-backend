package com.kospot.domain.statistic.repository;

import com.kospot.domain.game.vo.GameMode;

import com.kospot.domain.statistic.entity.GameModeStatistic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GameModeStatisticRepository extends JpaRepository<GameModeStatistic, Long> {

    @Query("SELECT g FROM GameModeStatistic g WHERE g.memberStatistic.id = :memberStatisticId AND g.gameMode = :gameMode")
    GameModeStatistic findByMemberStatisticIdAndGameMode(@Param("memberStatisticId") Long memberStatisticId, 
                                                          @Param("gameMode") GameMode gameMode);

    @Query("""
        SELECT g.gameMode, 
               SUM(g.practice.games + g.rank.games + g.multi.games) as totalGames,
               AVG(g.practice.avgScore) as avgPracticeScore,
               AVG(g.rank.avgScore) as avgRankScore,
               AVG(g.multi.avgScore) as avgMultiScore,
               SUM(g.multi.firstPlace) as totalFirstPlace,
               SUM(g.multi.secondPlace) as totalSecondPlace,
               SUM(g.multi.thirdPlace) as totalThirdPlace
        FROM GameModeStatistic g
        GROUP BY g.gameMode
        """)
    List<Object[]> getOverallStatisticsByMode();

    @Query("""
        SELECT g.gameMode, 
               SUM(g.practice.games + g.rank.games + g.multi.games) as totalGames,
               AVG(g.practice.avgScore) as avgPracticeScore,
               AVG(g.rank.avgScore) as avgRankScore,
               AVG(g.multi.avgScore) as avgMultiScore,
               SUM(g.multi.firstPlace) as totalFirstPlace,
               SUM(g.multi.secondPlace) as totalSecondPlace,
               SUM(g.multi.thirdPlace) as totalThirdPlace
        FROM GameModeStatistic g
        WHERE g.createdDate BETWEEN :startDate AND :endDate
        GROUP BY g.gameMode
        """)
    List<Object[]> getStatisticsByModeBetween(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT g FROM GameModeStatistic g
        WHERE g.gameMode = :gameMode
        ORDER BY 
            CASE :matchType
                WHEN 'PRACTICE' THEN g.practice.avgScore
                WHEN 'RANK' THEN g.rank.avgScore
                WHEN 'MULTI' THEN g.multi.avgScore
            END DESC
        """)
    Page<GameModeStatistic> findTopRankers(@Param("gameMode") GameMode gameMode,
                                            @Param("matchType") String matchType,
                                            Pageable pageable);

    @Query("""
        SELECT g.gameMode, COUNT(g)
        FROM GameModeStatistic g
        WHERE (g.practice.games + g.rank.games + g.multi.games) >= :minGames
        GROUP BY g.gameMode
        """)
    List<Object[]> countActivePlayers(@Param("minGames") long minGames);

    @Query(value = """
        SELECT 
            game_mode,
            FLOOR(practice_avg_score / 1000) * 1000 as score_range,
            COUNT(*) as player_count
        FROM game_mode_statistic
        WHERE practice_games > 0
        GROUP BY game_mode, score_range
        ORDER BY game_mode, score_range
        """, nativeQuery = true)
    List<Object[]> getScoreDistribution();

    @Query("""
        SELECT g.gameMode, SUM(g.practice.games + g.rank.games + g.multi.games)
        FROM GameModeStatistic g
        GROUP BY g.gameMode
        ORDER BY SUM(g.practice.games + g.rank.games + g.multi.games) DESC
        """)
    List<Object[]> getGameModePopularity();
}

