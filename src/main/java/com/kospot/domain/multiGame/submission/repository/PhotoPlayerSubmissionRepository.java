package com.kospot.domain.multiGame.submission.repository;

import com.kospot.domain.multiGame.submission.entity.photo.PhotoPlayerSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoPlayerSubmissionRepository extends JpaRepository<PhotoPlayerSubmission, Long> {
}
