package com.kospot.domain.multi.gamePlayer.repository;

import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {

    @Query("select count(gr) from GamePlayer gr where gr.multiRoadViewGame.id = :gameId")
    int countByMultiRoadViewGameId(@Param("gameId") Long id);

    @Query("SELECT gp FROM GamePlayer gp WHERE gp.multiRoadViewGame.id = :gameId")
    List<GamePlayer> findAllByMultiRoadViewGameId(@Param("gameId") Long gameId);
}
