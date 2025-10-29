package com.kospot.domain.gameconfig.adaptor;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gameconfig.entity.GameConfig;
import com.kospot.domain.gameconfig.repository.GameConfigRepository;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import com.kospot.infrastructure.exception.object.domain.GameConfigHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameConfigAdaptor {

    private final GameConfigRepository gameConfigRepository;

    public GameConfig queryById(Long id) {
        return gameConfigRepository.findById(id)
                .orElseThrow(() -> new GameConfigHandler(ErrorStatus.GAME_CONFIG_NOT_FOUND));
    }

    public List<GameConfig> queryAll() {
        return gameConfigRepository.findAll();
    }

    public List<GameConfig> queryAllActive() {
        return gameConfigRepository.findAllByIsActiveTrue();
    }

    public GameConfig queryByGameModeAndIsSingleMode(GameMode gameMode, Boolean isSingleMode) {
        return gameConfigRepository.findByGameModeAndIsSingleMode(gameMode, isSingleMode)
                .orElseThrow(() -> new GameConfigHandler(ErrorStatus.GAME_CONFIG_NOT_FOUND));
    }

    public GameConfig queryByGameModeAndPlayerMatchTypeAndIsSingleMode(
            GameMode gameMode,
            PlayerMatchType playerMatchType,
            Boolean isSingleMode
    ) {
        return gameConfigRepository.findByGameModeAndPlayerMatchTypeAndIsSingleMode(
                        gameMode, playerMatchType, isSingleMode)
                .orElseThrow(() -> new GameConfigHandler(ErrorStatus.GAME_CONFIG_NOT_FOUND));
    }
}

