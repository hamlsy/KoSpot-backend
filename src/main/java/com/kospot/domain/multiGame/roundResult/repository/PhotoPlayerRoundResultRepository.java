package com.kospot.domain.multiGame.roundResult.repository;

import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.roundResult.entity.PhotoPlayerRoundResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PhotoPlayerRoundResultRepository extends JpaRepository<PhotoPlayerRoundResult, Long> {

    @Query("SELECT pr FROM PhotoPlayerRoundResult pr WHERE pr.roundResult.game.id = :gameId AND pr.gamePlayer = :gamePlayer")
    List<PhotoPlayerRoundResult> findByGameIdAndGamePlayer(@Param("gameId") Long gameId, @Param("gamePlayer") GamePlayer gamePlayer);
    
    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN true ELSE false END FROM PhotoPlayerRoundResult pr " +
           "WHERE pr.roundResult.game.id = :gameId AND pr.roundResult.roundNumber = :roundNumber AND pr.gamePlayer = :gamePlayer")
    boolean existsByGameIdAndRoundNumberAndGamePlayer(@Param("gameId") Long gameId, 
                                                   @Param("roundNumber") Integer roundNumber, 
                                                   @Param("gamePlayer") GamePlayer gamePlayer);
} 