package com.kospot.domain.multi.submission.repository;

import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.submission.entity.roadview.RoadViewSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 로드뷰 제출 Repository (통합)
 * 
 * 장점:
 * - 단일 Repository로 모든 조회 처리
 * - 개인전/팀전 구분은 matchType으로
 * - 중복 쿼리 제거
 * 
 * 클린코드 원칙:
 * - 명확한 메서드 네이밍
 * - 단일 책임 원칙
 * - Query 메서드 네이밍 규칙 준수
 */
public interface RoadViewSubmissionRepository extends JpaRepository<RoadViewSubmission, Long> {

    // === 기본 조회 ===

    /**
     * 라운드별 전체 제출 조회 (개인전/팀전 모두, 순위 순)
     */
    List<RoadViewSubmission> findByRoundIdOrderByRankAsc(Long roundId);

    /**
     * 라운드별 제출 조회 (매치타입별, 순위 순)
     */
    List<RoadViewSubmission> findByRoundIdAndMatchTypeOrderByRankAsc(
            Long roundId,
            PlayerMatchType matchType
    );

    // === 카운트 조회 ===

    /**
     * 라운드별 제출 수 (개인전/팀전 통합)
     */
    @Query("SELECT COUNT(s) FROM RoadViewSubmission s WHERE s.round.id = :roundId")
    long countByRoundId(@Param("roundId") Long roundId);

    /**
     * 라운드별 제출 수 (매치타입별)
     */
    long countByRoundIdAndMatchType(Long roundId, PlayerMatchType matchType);

    // === 존재 여부 확인 ===

    /**
     * 개인전: 특정 플레이어 제출 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM RoadViewSubmission s " +
           "WHERE s.round.id = :roundId " +
           "AND s.matchType = 'SOLO' " +
           "AND s.gamePlayer.id = :playerId")
    boolean existsByRoundIdAndPlayerId(@Param("roundId") Long roundId, 
                                        @Param("playerId") Long playerId);

    /**
     * 팀전: 특정 팀 제출 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM RoadViewSubmission s " +
           "WHERE s.round.id = :roundId " +
           "AND s.matchType = 'TEAM' " +
           "AND s.teamNumber = :teamNumber")
    boolean existsByRoundIdAndTeamNumber(@Param("roundId") Long roundId,
                                          @Param("teamNumber") Integer teamNumber);

    // === 미제출자 조회 (조기 종료 및 0점 처리용) ===

    /**
     * 개인전: 미제출 플레이어 ID 조회
     * 
     * 사용처:
     * - 라운드 종료 시 미제출 플레이어 0점 처리
     * - EndRoadViewSoloRoundUseCase
     */
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

    /**
     * 팀전: 미제출 팀 번호 조회
     * 
     * 사용처:
     * - 라운드 종료 시 미제출 팀 0점 처리
     */
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

    /**
     * 개인전 제출만 조회
     * 
     * 사용처:
     * - 개인전 순위 계산
     * - EndRoadViewSoloRoundUseCase
     */
    @Query("SELECT s FROM RoadViewSubmission s " +
           "WHERE s.round.id = :roundId " +
           "AND s.matchType = 'SOLO' " +
           "ORDER BY s.distance ASC, s.timeToAnswer ASC")
    List<RoadViewSubmission> findSoloSubmissionsByRoundIdOrderByDistance(@Param("roundId") Long roundId);

    /**
     * 개인전 제출 (순위 순)
     */
    @Query("SELECT s FROM RoadViewSubmission s " +
           "WHERE s.round.id = :roundId " +
           "AND s.matchType = 'SOLO' " +
           "ORDER BY s.rank ASC")
    List<RoadViewSubmission> findSoloSubmissionsByRoundIdOrderByRank(@Param("roundId") Long roundId);

    // === 팀전 전용 조회 (하위 호환성) ===

    /**
     * 팀전 제출만 조회
     */
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

