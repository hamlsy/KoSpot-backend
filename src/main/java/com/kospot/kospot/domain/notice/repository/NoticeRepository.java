package com.kospot.kospot.domain.notice.repository;

import com.kospot.kospot.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}
