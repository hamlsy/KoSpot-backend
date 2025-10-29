package com.kospot.domain.gameconfig.service;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gameconfig.adaptor.GameConfigAdaptor;
import com.kospot.domain.gameconfig.entity.GameConfig;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameConfigService {

    private final GameConfigAdaptor gameConfigAdaptor;

    @Transactional
    public GameConfig createGameConfig(GameMode gameMode, PlayerMatchType playerMatchType, Boolean isSingleMode) {
        GameConfig gameConfig = GameConfig.builder()
                .gameMode(gameMode)
                .playerMatchType(playerMatchType)
                .isSingleMode(isSingleMode)
                .isActive(true)
                .build();
        return gameConfigAdaptor.save(gameConfig);
    }

    public GameConfig getGameConfig(Long id) {
        return gameConfigAdaptor.findById(id);
    }

    public List<GameConfig> getAllGameConfigs() {
        return gameConfigAdaptor.findAll();
    }

    public List<GameConfig> getActiveGameConfigs() {
        return gameConfigAdaptor.findAllActive();
    }

    @Transactional
    public void activateGameConfig(Long id) {
        GameConfig gameConfig = gameConfigAdaptor.findById(id);
        gameConfig.activate();
    }

    @Transactional
    public void deactivateGameConfig(Long id) {
        GameConfig gameConfig = gameConfigAdaptor.findById(id);
        gameConfig.deactivate();
    }

    @Transactional
    public void deleteGameConfig(Long id) {
        GameConfig gameConfig = gameConfigAdaptor.findById(id);
        gameConfigAdaptor.delete(gameConfig);
    }

    public boolean isGameModeActive(GameMode gameMode, Boolean isSingleMode) {
        try {
            GameConfig config = gameConfigAdaptor.findByGameModeAndIsSingleMode(gameMode, isSingleMode);
            return config.getIsActive();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isGameModeActive(GameMode gameMode, PlayerMatchType playerMatchType, Boolean isSingleMode) {
        try {
            GameConfig config = gameConfigAdaptor.findByGameModeAndPlayerMatchTypeAndIsSingleMode(
                    gameMode, playerMatchType, isSingleMode);
            return config.getIsActive();
        } catch (Exception e) {
            return false;
        }
    }
}

