package com.kospot.domain.multiGame.roundResult.repository;

import com.kospot.domain.multiGame.game.entity.MultiRoadViewGame;
import com.kospot.domain.multiGame.roundResult.entity.RoadViewRoundResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoadViewRoundResultRepository extends JpaRepository<RoadViewRoundResult, Long> {

    @Query("SELECT r FROM RoadViewRoundResult r WHERE r.game.id = :gameId")
    List<RoadViewRoundResult> findByGameId(@Param("gameId") Long gameId);
    
    @Query("SELECT r FROM RoadViewRoundResult r WHERE r.game.id = :gameId AND r.roundNumber = :roundNumber")
    Optional<RoadViewRoundResult> findByGameIdAndRoundNumber(@Param("gameId") Long gameId, @Param("roundNumber") Integer roundNumber);

} 