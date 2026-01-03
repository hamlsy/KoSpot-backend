package com.kospot.application.multi.game.strategy;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.service.MultiRoadViewGameService;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.service.GamePlayerService;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multi.game.dto.response.MultiGameResponse;
import com.kospot.presentation.multi.gamePlayer.dto.response.GamePlayerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component // NotifyStartGameUseCase가 GameMode/MATCH 조합에 맞는 전략을 선택할 때 사용
@Transactional
@RequiredArgsConstructor
public class RoadViewSoloStartStrategy implements MultiGameStartStrategy {

    private static final String TARGET_ROUTE = "ROADVIEW_GAME";

    private final MultiRoadViewGameService multiRoadViewGameService;
    private final GamePlayerService gamePlayerService;

    @Override
    public boolean supports(GameMode gameMode, PlayerMatchType matchType) {
        return GameMode.ROADVIEW.equals(gameMode) && PlayerMatchType.SOLO.equals(matchType);
    }

    @Override
    /**
     * 로드뷰 개인전 시작 컨텍스트를 구성해 게임, 플레이어 정보를 준비한다.
     */
    public StartGamePreparation prepare(GameRoom gameRoom,
                                        GameMode gameMode,
                                        PlayerMatchType matchType) {
        // 게임 생성
        MultiRoadViewGame game = multiRoadViewGameService.createGame(gameRoom);

        // 게임 플레이어 생성
        List<GamePlayer> gamePlayers = gamePlayerService.createRoadViewGamePlayers(gameRoom, game);

        List<GamePlayerResponse> players = gamePlayers.stream()
                .map(GamePlayerResponse::from)
                .toList();

        MultiGameResponse.StartGame startGame = MultiGameResponse.StartGame.builder()
                .gameId(game.getId())
                .gameMode(gameMode.name())
                .matchType(matchType.name())
                .totalRounds(game.getTotalRounds())
                .roundId(0L)
                .isPoiNameVisible(game.isPoiNameVisible())
                .currentRound(game.getCurrentRound())
                .roundTimeLimit(game.getTimeLimit())
                .players(players)
                .payload(null)
                .build();

        return new StartGamePreparation(startGame, TARGET_ROUTE, null);
    }
}

