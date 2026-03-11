package com.kospot.multi.submission.infrastructure.persistence;

import com.kospot.multi.submission.entity.photo.PhotoPlayerSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoPlayerSubmissionRepository extends JpaRepository<PhotoPlayerSubmission, Long> {
}
