package com.kospot.domain.multiGame.submittion.repository;

import com.kospot.domain.multiGame.submittion.entity.TeamSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamSubmissionRepository extends JpaRepository<TeamSubmission, Long> {
}
