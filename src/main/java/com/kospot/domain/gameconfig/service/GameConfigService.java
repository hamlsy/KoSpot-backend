package com.kospot.domain.gameconfig.service;

import com.kospot.domain.gameconfig.entity.GameConfig;
import com.kospot.domain.gameconfig.repository.GameConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GameConfigService {

    private final GameConfigRepository gameConfigRepository;

    public GameConfig createGameConfig(GameConfig gameConfig) {
        return gameConfigRepository.save(gameConfig);
    }

    public void activateGameConfig(GameConfig gameConfig) {
        gameConfig.activate();
    }

    public void deactivateGameConfig(GameConfig gameConfig) {
        gameConfig.deactivate();
    }

    public void deleteGameConfig(GameConfig gameConfig) {
        gameConfigRepository.delete(gameConfig);
    }
}

