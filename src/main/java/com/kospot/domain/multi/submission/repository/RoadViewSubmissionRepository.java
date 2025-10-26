package com.kospot.domain.multi.submission.repository;

import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.submission.entity.roadview.RoadViewSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoadViewSubmissionRepository extends JpaRepository<RoadViewSubmission, Long> {

    List<RoadViewSubmission> findByRoundId(Long roundId);

    List<RoadViewSubmission> findByRoundIdOrderByRankAsc(Long roundId);

    List<RoadViewSubmission> findByRoundIdAndMatchTypeOrderByRankAsc(
            Long roundId,
            PlayerMatchType matchType
    );

    @Query("SELECT s FROM RoadViewSubmission s " +
           "JOIN FETCH s.gamePlayer gp " +
           "WHERE s.round.id = :id")
    List<RoadViewSubmission> findByRoundIdFetchGamePlayer(@Param("id") Long roundId);

    // === 카운트 조회 ===
    @Query("SELECT COUNT(s) FROM RoadViewSubmission s WHERE s.round.id = :roundId")
    long countByRoundId(@Param("roundId") Long roundId);

    @Query("SELECT COUNT(s) FROM RoadViewSubmission s WHERE s.round.id = :roundId AND s.matchType = :matchType")
    long countByRoundIdAndMatchType(@Param("roundId") Long roundId, @Param("matchType") PlayerMatchType matchType);

    // === 존재 여부 확인 ===

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM RoadViewSubmission s " +
           "WHERE s.round.id = :roundId " +
           "AND s.matchType = 'SOLO' " +
           "AND s.gamePlayer.id = :playerId")
    boolean existsByRoundIdAndPlayerId(@Param("roundId") Long roundId, 
                                        @Param("playerId") Long playerId);


    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM RoadViewSubmission s " +
           "WHERE s.round.id = :roundId " +
           "AND s.matchType = 'TEAM' " +
           "AND s.teamNumber = :teamNumber")
    boolean existsByRoundIdAndTeamNumber(@Param("roundId") Long roundId,
                                          @Param("teamNumber") Integer teamNumber);

    // === 미제출자 조회 (조기 종료 및 0점 처리용) ===
    @Query("SELECT gp.id FROM GamePlayer gp " +
           "WHERE gp.multiRoadViewGame.id = :gameId " +
           "AND gp.id NOT IN (" +
           "    SELECT s.gamePlayer.id FROM RoadViewSubmission s " +
           "    WHERE s.round.id = :roundId " +
           "    AND s.matchType = 'SOLO' " +
           "    AND s.gamePlayer IS NOT NULL" +
           ")")
    List<Long> findNonSubmittedPlayerIds(@Param("gameId") Long gameId,
                                          @Param("roundId") Long roundId);

    @Query("SELECT DISTINCT gp.teamNumber FROM GamePlayer gp " +
           "WHERE gp.multiRoadViewGame.id = :gameId " +
           "AND gp.teamNumber IS NOT NULL " +
           "AND gp.teamNumber NOT IN (" +
           "    SELECT s.teamNumber FROM RoadViewSubmission s " +
           "    WHERE s.round.id = :roundId " +
           "    AND s.matchType = 'TEAM' " +
           "    AND s.teamNumber IS NOT NULL" +
           ")")
    List<Integer> findNonSubmittedTeamNumbers(@Param("gameId") Long gameId,
                                               @Param("roundId") Long roundId);

    // === 개인전 전용 조회 (하위 호환성) ===
    @Query("SELECT s FROM RoadViewSubmission s " +
           "WHERE s.round.id = :roundId " +
           "AND s.matchType = 'SOLO' " +
           "ORDER BY s.distance ASC, s.timeToAnswer ASC")
    List<RoadViewSubmission> findSoloSubmissionsByRoundIdOrderByDistance(@Param("roundId") Long roundId);

    @Query("SELECT s FROM RoadViewSubmission s " +
           "WHERE s.round.id = :roundId " +
           "AND s.matchType = 'SOLO' " +
           "ORDER BY s.rank ASC")
    List<RoadViewSubmission> findSoloSubmissionsByRoundIdOrderByRank(@Param("roundId") Long roundId);

    // === 팀전 전용 조회 (하위 호환성) ===
    @Query("SELECT s FROM RoadViewSubmission s " +
           "WHERE s.round.id = :roundId " +
           "AND s.matchType = 'TEAM' " +
           "ORDER BY s.distance ASC, s.timeToAnswer ASC")
    List<RoadViewSubmission> findTeamSubmissionsByRoundIdOrderByDistance(@Param("roundId") Long roundId);

    /**
     * 팀전 제출 (순위 순)
     */
    @Query("SELECT s FROM RoadViewSubmission s " +
           "WHERE s.round.id = :roundId " +
           "AND s.matchType = 'TEAM' " +
           "ORDER BY s.rank ASC")
    List<RoadViewSubmission> findTeamSubmissionsByRoundIdOrderByRank(@Param("roundId") Long roundId);

    // === 통계 조회 ===

    /**
     * 라운드별 평균 거리
     */
    @Query("SELECT AVG(s.distance) FROM RoadViewSubmission s " +
           "WHERE s.round.id = :roundId " +
           "AND s.matchType = :matchType")
    Double findAverageDistanceByRoundIdAndMatchType(@Param("roundId") Long roundId,
                                                     @Param("matchType") PlayerMatchType matchType);

    /**
     * 라운드별 평균 응답 시간
     */
    @Query("SELECT AVG(s.timeToAnswer) FROM RoadViewSubmission s " +
           "WHERE s.round.id = :roundId " +
           "AND s.matchType = :matchType")
    Double findAverageTimeByRoundIdAndMatchType(@Param("roundId") Long roundId,
                                                 @Param("matchType") PlayerMatchType matchType);
}

