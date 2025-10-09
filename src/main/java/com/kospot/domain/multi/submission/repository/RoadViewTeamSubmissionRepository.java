package com.kospot.domain.multi.submission.repository;

import com.kospot.domain.multi.submission.entity.roadView.RoadViewTeamSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoadViewTeamSubmissionRepository extends JpaRepository<RoadViewTeamSubmission, Long> {

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM RoadViewTeamSubmission s " +
           "WHERE s.roadViewGameRound.id = :roundId AND s.teamNumber = :teamNumber")
    boolean existsByRoundIdAndTeamNumber(@Param("roundId") Long roundId, @Param("teamNumber") Integer teamNumber);
}
