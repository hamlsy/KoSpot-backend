package com.kospot.multi.game.application.strategy;

import com.kospot.game.domain.vo.GameMode;
import com.kospot.multi.game.domain.factory.GameCreationResult;
import com.kospot.multi.game.domain.factory.MultiRoadViewGameFactory;
import com.kospot.multi.game.domain.vo.PlayerMatchType;
import com.kospot.multi.room.domain.entity.GameRoom;
import com.kospot.multi.game.presentation.dto.response.MultiGameResponse;
import com.kospot.multi.game.presentation.mapper.MultiGameResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로드뷰 개인전 게임 시작 전략
 * Factory를 통해 게임과 플레이어를 생성하고,
 * Mapper를 통해 응답 DTO를 구성한다.
 */
@Component
@Transactional
@RequiredArgsConstructor
public class RoadViewSoloStartStrategy implements MultiGameStartStrategy {

    private static final String TARGET_ROUTE = "ROADVIEW_GAME";

    private final MultiRoadViewGameFactory gameFactory;
    private final MultiGameResponseMapper responseMapper;

    @Override
    public boolean supports(GameMode gameMode, PlayerMatchType matchType) {
        return GameMode.ROADVIEW.equals(gameMode) && PlayerMatchType.SOLO.equals(matchType);
    }

    /**
     * 로드뷰 개인전 시작 컨텍스트를 구성해 게임, 플레이어 정보를 준비
     * 로드뷰 개인전 시작 컨텍스트를 구성해 게임, 플레이어 정보를 준비한다.
     * Factory로 게임/플레이어 생성을 위임하고, Mapper로 DTO 변환을 위임한다.
     */
    @Override
    public StartGamePreparation prepare(GameRoom gameRoom,
                                        GameMode gameMode,
                                        PlayerMatchType matchType) {
        // Factory를 통한 게임 및 플레이어 생성
        GameCreationResult creationResult = gameFactory.createGameWithPlayers(gameRoom);

        // Mapper를 통한 응답 DTO 생성
        MultiGameResponse.StartGame startGame = responseMapper.toStartGameResponse(
                creationResult, gameMode, matchType);

        return new StartGamePreparation(startGame, TARGET_ROUTE, null);
    }
}
