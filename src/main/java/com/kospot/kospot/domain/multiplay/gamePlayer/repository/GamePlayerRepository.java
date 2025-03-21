package com.kospot.kospot.domain.multiplay.gamePlayer.repository;

import com.kospot.kospot.domain.multiplay.gamePlayer.entity.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {
}
