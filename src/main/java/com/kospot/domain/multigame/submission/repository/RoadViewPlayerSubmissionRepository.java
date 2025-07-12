package com.kospot.domain.multigame.submission.repository;

import com.kospot.domain.multigame.submission.entity.roadView.RoadViewPlayerSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoadViewPlayerSubmissionRepository extends JpaRepository<RoadViewPlayerSubmission, Long> {

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM RoadViewPlayerSubmission s " +
           "WHERE s.roadViewGameRound.id = :roundId AND s.gamePlayer.id = :playerId")
    boolean existsByRoundIdAndGamePlayerId(@Param("roundId") Long roundId, @Param("playerId") Long playerId);
    
}
