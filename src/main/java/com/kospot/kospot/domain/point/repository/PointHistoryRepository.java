package com.kospot.kospot.domain.point.repository;

import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.awt.print.Pageable;
import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    @Query("select p from PointHistory p join fetch p.member where p.member.id = :memberId")
    List<PointHistory> findAllByMemberId(@Param("memberId") Long memberId);

    @Query("select p from PointHistory p where p.member = member")
    List<PointHistory> findAllByMemberIdPaging(@Param("member")Member member, Pageable pageable);


}
