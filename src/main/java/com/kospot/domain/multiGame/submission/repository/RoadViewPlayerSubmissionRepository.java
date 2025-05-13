package com.kospot.domain.multiGame.submission.repository;

import com.kospot.domain.multiGame.submission.entity.roadView.RoadViewPlayerSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadViewPlayerSubmissionRepository extends JpaRepository<RoadViewPlayerSubmission, Long> {

    boolean existsByRoundIdAndGamePlayerId(Long roundId, Long playerId);
    
}
