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

    @Query("select gp from GamePlayer gp where gp.member.id = :memberId")
    Optional<GamePlayer> findByMemberId(@Param("memberId") Long memberId);

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

    @Query("select count(gp) from GamePlayer gp where gp.member.id = :memberId")
    long countByMemberId(@Param("memberId") Long memberId);

    @Query("select coalesce(avg(gp.totalScore), 0.0) from GamePlayer gp where gp.member.id = :memberId")
    double findAverageScoreByMemberId(@Param("memberId") Long memberId);

    @Query("select count(gp) from GamePlayer gp where gp.member.id = :memberId and gp.roundRank = :rank and gp.status = 'FINISHED'")
    long countByMemberIdAndRank(@Param("memberId") Long memberId, @Param("rank") Integer rank);

    @Query("select gp.createdDate from GamePlayer gp where gp.member.id = :memberId order by gp.createdDate desc")
    List<java.time.LocalDateTime> findAllCreatedDatesByMemberId(@Param("memberId") Long memberId);

    @Query("select gp.createdDate from GamePlayer gp where gp.member.id = :memberId and gp.status = 'FINISHED' order by gp.createdDate desc limit 1")
    java.time.LocalDateTime findLatestPlayDateByMemberId(@Param("memberId") Long memberId);
}
