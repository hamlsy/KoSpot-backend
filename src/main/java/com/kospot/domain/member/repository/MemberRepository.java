package com.kospot.domain.member.repository;

import com.kospot.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findAllByGameRoomId(Long gameRoomId);

    @Query("select m from Member m left join fetch m.equippedMarkerImage where m.username = :username")
    Optional<Member> findByUsernameFetchEquippedMarkerImage(@Param("username") String username);

    Optional<Member> findByUsername(String username);

}
