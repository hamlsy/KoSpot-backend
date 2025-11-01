package com.kospot.domain.game.repository;

import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.vo.GameStatus;
import com.kospot.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoadViewGameRepository extends JpaRepository<RoadViewGame, Long> {

    @Query("select rg from RoadViewGame rg where rg.member.id = :memberId")
    List<RoadViewGame> findByMemberId(@Param("memberId") Long memberId);

    @Query("select rg from RoadViewGame rg where rg.member.email = :memberEmail")
    List<RoadViewGame> findByMemberEmail(@Param("memberEmail") String memberEmail);

    @Query("select rg from RoadViewGame rg join fetch rg.coordinate where rg.id = :id")
    Optional<RoadViewGame> findByIdFetchCoordinate(@Param("id") Long id);

    @Query("select rg from RoadViewGame rg " +
            "join fetch rg.coordinate " +
            "where rg.member = :member " +
            "and rg.gameStatus = :gameStatus " +
            "order by rg.createdDate desc")
    List<RoadViewGame> findTop3ByMemberAndGameStatusOrderByCreatedAtDesc(
            @Param("member") Member member,
            @Param("gameStatus") GameStatus gameStatus,
            Pageable pageable
    );

    @Query(value = "select rg from RoadViewGame rg " +
            "join fetch rg.coordinate " +
            "where rg.member = :member " +
            "and rg.gameStatus = :gameStatus " +
            "order by rg.createdDate desc",
            countQuery = "select count(rg) from RoadViewGame rg " +
                    "where rg.member = :member " +
                    "and rg.gameStatus = :gameStatus")
    Page<RoadViewGame> findByMemberAndGameStatusOrderByCreatedAtDesc(
            @Param("member") Member member,
            @Param("gameStatus") GameStatus gameStatus,
            Pageable pageable
    );
}
