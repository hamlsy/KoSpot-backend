package com.kospot.domain.gameconfig.repository;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gameconfig.entity.GameConfig;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameConfigRepository extends JpaRepository<GameConfig, Long> {

    List<GameConfig> findAllByIsActiveTrue();

    Optional<GameConfig> findByGameModeAndIsSingleMode(GameMode gameMode, Boolean isSingleMode);

    Optional<GameConfig> findByGameModeAndPlayerMatchTypeAndIsSingleMode(
            GameMode gameMode,
            PlayerMatchType playerMatchType,
            Boolean isSingleMode
    );
}

