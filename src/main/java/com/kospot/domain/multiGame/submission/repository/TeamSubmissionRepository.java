package com.kospot.domain.multiGame.submission.repository;

import com.kospot.domain.multiGame.submission.entity.roadView.RoadViewTeamSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamSubmissionRepository extends JpaRepository<RoadViewTeamSubmission, Long> {
}
