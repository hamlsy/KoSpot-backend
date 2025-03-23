package com.kospot.kospot.domain.multiplay.gameRoom.repository;

import com.kospot.kospot.domain.multiplay.gameRoom.entity.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {

    @Query("select gr from GameRoom gr join fetch gr.waitingPlayers where gr.id = :id")
    Optional<GameRoom> findByIdFetchPlayers(@Param("id") Long id);

}
