package com.kospot.domain.statistic.repository;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.statistic.entity.MemberStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberStatisticRepository extends JpaRepository<MemberStatistic, Long> {

    @Query("select ms from MemberStatistic ms where ms.member = :member")
    Optional<MemberStatistic> findByMember(@Param("member") Member member);

    @Query("select ms from MemberStatistic ms where ms.member.id = :memberId")
    Optional<MemberStatistic> findByMemberId(@Param("memberId") Long memberId);
}

