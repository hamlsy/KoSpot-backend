package com.kospot.domain.multi.round.repository;

import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 로드뷰 게임 라운드 Repository
 * 
 * 리팩토링:
 * - Player/Team Submission 분리 쿼리 통합
 * - 단일 submissions 리스트 Fetch 쿼리
 */
public interface RoadViewGameRoundRepository extends JpaRepository<RoadViewGameRound, Long> {
    
    List<RoadViewGameRound> findAllByMultiRoadViewGameId(Long gameId);

    /**
     * 제출 데이터와 플레이어 정보 함께 조회 (통합)
     * - 개인전/팀전 모두 포함
     * - GamePlayer도 함께 Fetch (N+1 방지)
     */
    @Query("SELECT DISTINCT r FROM RoadViewGameRound r " +
           "LEFT JOIN FETCH r.roadViewSubmissions s " +
           "LEFT JOIN FETCH s.gamePlayer gp " +
           "WHERE r.id = :id")
    Optional<RoadViewGameRound> findByIdFetchSubmissionsAndPlayers(@Param("id") Long id);

    /**
     * 제출 데이터만 조회 (통합)
     */
    @Query("SELECT DISTINCT r FROM RoadViewGameRound r " +
           "LEFT JOIN FETCH r.roadViewSubmissions " +
           "WHERE r.id = :id")
    Optional<RoadViewGameRound> findByIdFetchSubmissions(@Param("id") Long id);

    /**
     * @deprecated Use findByIdFetchSubmissionsAndPlayers() instead
     * 하위 호환성을 위해 유지
     */
    @Deprecated
    default Optional<RoadViewGameRound> findByIdFetchPlayerSubmissionAndPlayers(Long id) {
        return findByIdFetchSubmissionsAndPlayers(id);
    }

    /**
     * @deprecated Use findByIdFetchSubmissions() instead
     * 하위 호환성을 위해 유지
     */
    @Deprecated
    default Optional<RoadViewGameRound> findByIdFetchPlayerSubmission(Long id) {
        return findByIdFetchSubmissions(id);
    }

    /**
     * @deprecated Use findByIdFetchSubmissions() instead
     * 하위 호환성을 위해 유지
     */
    @Deprecated
    default Optional<RoadViewGameRound> findByIdFetchTeamSubmissions(Long id) {
        return findByIdFetchSubmissions(id);
    }
}
