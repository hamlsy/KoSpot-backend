package com.kospot.domain.notice.repository;

import com.kospot.domain.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Override
    Page<Notice> findAll(Pageable pageable);

    @Query("select n from Notice n left join fetch n.images where n.id = :id")
    Optional<Notice> findByIdFetchImage(@Param("id") Long id);
}
