package com.kospot.domain.multiGame.gamePlayer.repository;

import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {


    int countByGameRoomId(Long id);
}
