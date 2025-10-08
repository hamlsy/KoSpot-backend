package com.kospot.domain.multi.submission.repository;

import com.kospot.domain.multi.submission.entity.roadView.RoadViewTeamSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadViewTeamSubmissionRepository extends JpaRepository<RoadViewTeamSubmission, Long> {
}
