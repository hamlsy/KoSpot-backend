package com.kospot.domain.member.repository;

import com.kospot.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findAllByGameRoomId(Long gameRoomId);

}
