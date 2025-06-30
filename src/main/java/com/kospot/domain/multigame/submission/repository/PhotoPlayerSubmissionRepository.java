package com.kospot.domain.multigame.submission.repository;

import com.kospot.domain.multigame.submission.entity.photo.PhotoPlayerSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoPlayerSubmissionRepository extends JpaRepository<PhotoPlayerSubmission, Long> {
}
