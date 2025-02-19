package com.kospot.kospot.domain.point.repository;

import com.kospot.kospot.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    @Query("select p from PointHistory p join fetch p.member.id where p.member = :memberId")
    List<PointHistory> findAllByMemberId(Long memberId);

}
