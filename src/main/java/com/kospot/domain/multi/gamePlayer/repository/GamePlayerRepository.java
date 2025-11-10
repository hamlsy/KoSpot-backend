package com.kospot.domain.multi.gamePlayer.repository;

import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {

    @Query("select count(gr) from GamePlayer gr where gr.multiRoadViewGame.id = :gameId")
    int countByMultiRoadViewGameId(@Param("gameId") Long id);

    @Query("select gp from GamePlayer gp where gp.member.id = :memberId and gp.multiRoadViewGame.id = :gameId")
    Optional<GamePlayer> findByMemberIdAndMultiGameId(@Param("memberId") Long memberId, @Param("gameId") Long gameId);

    @Query("SELECT gp FROM GamePlayer gp WHERE gp.multiRoadViewGame.id = :gameId")
    List<GamePlayer> findAllByMultiRoadViewGameId(@Param("gameId") Long gameId);

    @Query("SELECT gp FROM GamePlayer gp " +
           "JOIN FETCH gp.member m " +
           "JOIN FETCH m.equippedMarkerImage " +
           "WHERE gp.multiRoadViewGame.id = :gameId")
    List<GamePlayer> findAllByMultiRoadViewGameIdWithMember(@Param("gameId") Long gameId);

    @Query("SELECT gp FROM GamePlayer gp WHERE gp.multiRoadViewGame.id = :gameId AND gp.teamNumber = :teamNumber")
    List<GamePlayer> findAllByMultiRoadViewGameIdAndTeamNumber(
            @Param("gameId") Long gameId,
            @Param("teamNumber") Integer teamNumber
    );

    @Query("SELECT COUNT(DISTINCT gp.teamNumber) FROM GamePlayer gp " +
           "WHERE gp.multiRoadViewGame.id = :gameId " +
           "AND gp.teamNumber IS NOT NULL")
    int countDistinctTeamsByGameId(@Param("gameId") Long gameId);
}
