package com.kospot.domain.game.repository;

import com.kospot.domain.game.entity.RoadViewGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.util.List;
import java.util.Optional;

public interface RoadViewGameRepository extends JpaRepository<RoadViewGame, Long> {

    @Query("select rg from RoadViewGame rg where rg.member.id = :memberId")
    List<RoadViewGame> findByMemberId(@Param("memberId") Long memberId);

    @Query("select rg from RoadViewGame rg where rg.member.email = :memberEmail")
    List<RoadViewGame> findByMemberEmail(@Param("memberEmail") String memberEmail);

    @Query("select rg from RoadViewGame rg join fetch rg.coordinate where rg.id = :id")
    Optional<RoadViewGame> findByIdFetchCoordinate(@Param("id") Long id);
}
