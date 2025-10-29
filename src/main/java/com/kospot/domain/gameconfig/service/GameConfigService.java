package com.kospot.domain.gameconfig.service;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gameconfig.entity.GameConfig;
import com.kospot.domain.gameconfig.repository.GameConfigRepository;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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

    /**
     * 모든 기본 게임 설정이 존재하는지 확인하고, 없으면 생성합니다.
     * 기본값은 모두 활성화 상태입니다.
     */
    public void ensureDefaultConfigsExist() {
        // 싱글 모드 설정 확인 및 생성
        for (GameMode gameMode : GameMode.values()) {
            ensureConfigExists(gameMode, null, true);
        }

        // 멀티 모드 설정 확인 및 생성
        for (GameMode gameMode : GameMode.values()) {
            for (PlayerMatchType matchType : PlayerMatchType.values()) {
                ensureConfigExists(gameMode, matchType, false);
            }
        }
    }

    private void ensureConfigExists(GameMode gameMode, PlayerMatchType playerMatchType, boolean isSingleMode) {
        boolean exists = gameConfigRepository.findByGameModeAndPlayerMatchTypeAndIsSingleMode(
                gameMode, playerMatchType, isSingleMode
        ).isPresent();

        if (!exists) {
            GameConfig config = GameConfig.builder()
                    .gameMode(gameMode)
                    .playerMatchType(playerMatchType)
                    .isSingleMode(isSingleMode)
                    .isActive(true) // 기본값: 활성화
                    .build();
            gameConfigRepository.save(config);
            
            log.info("GameConfig 자동 생성: gameMode={}, playerMatchType={}, isSingleMode={}", 
                    gameMode, playerMatchType, isSingleMode);
        }
    }
}

