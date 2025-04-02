package com.kospot.domain.multiGame.game.repository;

import com.kospot.domain.multiGame.game.entity.MultiRoadViewGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MultiRoadViewGameRepository extends JpaRepository<MultiRoadViewGame, Long> {
    
    @Query("SELECT g FROM MultiRoadViewGame g WHERE g.gameRoom.id = :gameRoomId")
    Optional<MultiRoadViewGame> findByGameRoomId(@Param("gameRoomId") Long gameRoomId);
    
    @Query("SELECT g FROM MultiRoadViewGame g LEFT JOIN FETCH g.gameRounds WHERE g.id = :gameId")
    Optional<MultiRoadViewGame> findWithRoundsById(@Param("gameId") Long gameId);
} 