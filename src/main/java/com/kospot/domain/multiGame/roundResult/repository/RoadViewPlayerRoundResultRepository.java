package com.kospot.domain.multiGame.roundResult.repository;

import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.roundResult.entity.RoadViewPlayerRoundResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoadViewPlayerRoundResultRepository extends JpaRepository<RoadViewPlayerRoundResult, Long> {

    @Query("SELECT pr FROM RoadViewPlayerRoundResult pr WHERE pr.roundResult.game.id = :gameId AND pr.gamePlayer = :gamePlayer")
    List<RoadViewPlayerRoundResult> findByGameIdAndGamePlayer(@Param("gameId") Long gameId, @Param("gamePlayer") GamePlayer gamePlayer);
    
    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN true ELSE false END FROM RoadViewPlayerRoundResult pr " +
           "WHERE pr.roundResult.game.id = :gameId AND pr.roundResult.roundNumber = :roundNumber AND pr.gamePlayer = :gamePlayer")
    boolean existsByGameIdAndRoundNumberAndGamePlayer(@Param("gameId") Long gameId, 
                                                   @Param("roundNumber") Integer roundNumber, 
                                                   @Param("gamePlayer") GamePlayer gamePlayer);
} 