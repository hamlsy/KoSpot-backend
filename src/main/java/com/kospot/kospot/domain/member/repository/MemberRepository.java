package com.kospot.kospot.domain.member.repository;

import com.kospot.kospot.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
