package com.kospot.multi.game.application.strategy;

import com.kospot.game.domain.vo.GameMode;
import com.kospot.multi.game.domain.vo.PlayerMatchType;
import com.kospot.multi.room.domain.entity.GameRoom;
import com.kospot.multi.game.presentation.dto.response.MultiGameResponse;

public interface MultiGameStartStrategy { // 게임 시작 컨텍스트를 모드별로 준비하기 위한 전략 인터페이스

    boolean supports(GameMode gameMode, PlayerMatchType matchType);

    StartGamePreparation prepare(GameRoom gameRoom,
                                 GameMode gameMode,
                                 PlayerMatchType matchType);

    record StartGamePreparation(MultiGameResponse.StartGame startGame,
                                String targetRoute,
                                Long countdownMs) {
    }
}

