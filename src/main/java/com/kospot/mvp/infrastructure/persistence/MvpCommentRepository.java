package com.kospot.mvp.infrastructure.persistence;

import com.kospot.mvp.domain.entity.MvpComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface MvpCommentRepository extends JpaRepository<MvpComment, Long> {

    Page<MvpComment> findByDailyMvpIdOrderByCreatedDateDesc(Long dailyMvpId, Pageable pageable);

    long countByDailyMvp_MvpDate(LocalDate mvpDate);
}
