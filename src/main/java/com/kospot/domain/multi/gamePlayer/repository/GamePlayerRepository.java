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

    @Query("select gp from GamePlayer gp where gp.memberId = :memberId")
    Optional<GamePlayer> findByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT gp FROM GamePlayer gp WHERE gp.multiRoadViewGame.id = :gameId")
    List<GamePlayer> findAllByMultiRoadViewGameId(@Param("gameId") Long gameId);

    @Query("SELECT gp FROM GamePlayer gp WHERE gp.multiRoadViewGame.id = :gameId AND gp.teamNumber = :teamNumber")
    List<GamePlayer> findAllByMultiRoadViewGameIdAndTeamNumber(
            @Param("gameId") Long gameId,
            @Param("teamNumber") Integer teamNumber
    );

    /**
     * 게임 내 팀 수 계산
     * 팀전에서 제출 완료 여부를 확인할 때 사용
     */
    @Query("SELECT COUNT(DISTINCT gp.teamNumber) FROM GamePlayer gp " +
           "WHERE gp.multiRoadViewGame.id = :gameId " +
           "AND gp.teamNumber IS NOT NULL")
    int countDistinctTeamsByGameId(@Param("gameId") Long gameId);
}
