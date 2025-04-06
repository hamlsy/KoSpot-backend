package com.kospot.domain.multiGame.roundResult.repository;

import com.kospot.domain.multiGame.game.entity.MultiGame;
import com.kospot.domain.multiGame.roundResult.entity.RoundResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoundResultRepository extends JpaRepository<RoundResult, Long> {
    
    @Query("SELECT g FROM MultiGame g WHERE g.id = :gameId")
    Optional<MultiGame> findGameById(@Param("gameId") Long gameId);
    
    List<RoundResult> findByGameId(Long gameId);
    
    @Query("SELECT r FROM RoundResult r WHERE r.game.id = :gameId AND r.roundNumber = :roundNumber")
    Optional<RoundResult> findByGameIdAndRoundNumber(@Param("gameId") Long gameId, @Param("roundNumber") Integer roundNumber);
} 