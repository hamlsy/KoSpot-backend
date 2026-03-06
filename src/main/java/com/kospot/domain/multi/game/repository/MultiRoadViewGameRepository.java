package com.kospot.domain.multi.game.repository;

import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.vo.MultiGameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MultiRoadViewGameRepository extends JpaRepository<MultiRoadViewGame, Long> {

    List<MultiRoadViewGame> findAllByGameRoomId(Long id);

    @Query("SELECT g FROM MultiRoadViewGame g WHERE g.gameRoomId = :gameRoomId AND g.status = 'IN_PROGRESS'")
    Optional<MultiRoadViewGame> findInProgressByGameRoomId(@Param("gameRoomId") Long gameRoomId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MultiRoadViewGame g " +
            "SET g.status = :nextStatus, g.currentRound = 1, g.isFinished = false " +
            "WHERE g.id = :gameId AND g.status = :expectedStatus")
    int transitionToInProgressIfPending(@Param("gameId") Long gameId,
                                        @Param("expectedStatus") MultiGameStatus expectedStatus,
                                        @Param("nextStatus") MultiGameStatus nextStatus);

}
