package com.kospot.domain.multigame.game.repository;

import com.kospot.domain.multigame.game.entity.MultiRoadViewGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MultiRoadViewGameRepository extends JpaRepository<MultiRoadViewGame, Long> {
    
    @Query("SELECT g FROM MultiRoadViewGame g WHERE g.gameRoom.id = :gameRoomId")
    Optional<MultiRoadViewGame> findByGameRoomId(@Param("gameRoomId") Long gameRoomId);
    
    @Query("SELECT g FROM MultiRoadViewGame g LEFT JOIN FETCH g.roadViewGameRounds WHERE g.id = :gameId")
    Optional<MultiRoadViewGame> findWithRoundsById(@Param("gameId") Long gameId);

    @Query("SELECT g FROM MultiRoadViewGame g JOIN FETCH g.gameRoom WHERE g.id = :gameId")
    Optional<MultiRoadViewGame> findByIdFetchGameRoom(@Param("gameId") Long gameId);

    List<MultiRoadViewGame> findAllByGameRoomId(Long id);


}