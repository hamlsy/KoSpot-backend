package com.kospot.domain.game.repository;

import com.kospot.domain.game.entity.RoadViewGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoadViewGameRepository extends JpaRepository<RoadViewGame, Long> {

    @Query("select rg from RoadViewGame rg where rg.member.id = :memberId")
    List<RoadViewGame> findByMemberId(@Param("memberId") Long memberId);

    @Query("select rg from RoadViewGame rg where rg.member.email = :memberEmail")
    List<RoadViewGame> findByMemberEmail(@Param("memberEmail") String memberEmail);
}
