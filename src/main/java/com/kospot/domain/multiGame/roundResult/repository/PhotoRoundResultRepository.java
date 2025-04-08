package com.kospot.domain.multiGame.roundResult.repository;

import com.kospot.domain.multiGame.game.entity.MultiPhotoGame;
import com.kospot.domain.multiGame.roundResult.entity.PhotoRoundResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PhotoRoundResultRepository extends JpaRepository<PhotoRoundResult, Long> {

    @Query("SELECT r FROM PhotoRoundResult r WHERE r.game.id = :gameId")
    List<PhotoRoundResult> findByGameId(@Param("gameId") Long gameId);
    
    @Query("SELECT r FROM PhotoRoundResult r WHERE r.game.id = :gameId AND r.roundNumber = :roundNumber")
    Optional<PhotoRoundResult> findByGameIdAndRoundNumber(@Param("gameId") Long gameId, @Param("roundNumber") Integer roundNumber);
    
    @Query("SELECT g FROM MultiPhotoGame g WHERE g.id = :gameId")
    Optional<MultiPhotoGame> findGameById(@Param("gameId") Long gameId);
} 