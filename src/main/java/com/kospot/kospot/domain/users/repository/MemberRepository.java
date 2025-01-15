package com.kospot.kospot.domain.users.repository;

import com.kospot.kospot.domain.users.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
