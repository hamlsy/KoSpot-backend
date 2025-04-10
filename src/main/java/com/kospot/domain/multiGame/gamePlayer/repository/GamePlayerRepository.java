package com.kospot.domain.multiGame.gamePlayer.repository;

import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {

    @Query("select gr from GamePlayer gr where gr.gameRoom.id = :gameRoomId")
    List<GamePlayer> findAllByGameRoomId(@Param("gameRoomId") Long gameRoomId);

    int countByGameRoomId(Long id);
}
