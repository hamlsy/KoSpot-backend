package com.kospot.application.multi.game.strategy;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multi.game.dto.response.MultiGameResponse;

public interface MultiGameStartStrategy { // 게임 시작 컨텍스트를 모드별로 준비하기 위한 전략 인터페이스

    boolean supports(GameMode gameMode, PlayerMatchType matchType);

    StartGamePreparation prepare(GameRoom gameRoom,
                                 MultiGameRequest.Start request,
                                 GameMode gameMode,
                                 PlayerMatchType matchType);

    record StartGamePreparation(MultiGameResponse.StartGame startGame,
                                String targetRoute,
                                Long countdownMs) {
    }
}

