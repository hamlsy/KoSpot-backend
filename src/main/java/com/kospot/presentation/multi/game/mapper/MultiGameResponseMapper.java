package com.kospot.presentation.multi.game.mapper;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.factory.GameCreationResult;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.presentation.multi.game.dto.response.MultiGameResponse;
import com.kospot.presentation.multi.gamePlayer.dto.response.GamePlayerResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 멀티 게임 관련 Response DTO 변환을 담당하는 Mapper
 * Strategy나 UseCase에서 DTO 생성 로직을 분리한다.
 */
@Component
public class MultiGameResponseMapper {

    private static final Long INITIAL_ROUND_ID = 0L;

    /**
     * GameCreationResult를 StartGame 응답으로 변환한다.
     *
     * @param result    게임 생성 결과
     * @param gameMode  게임 모드
     * @param matchType 매치 타입
     * @return 게임 시작 응답 DTO
     */
    public MultiGameResponse.StartGame toStartGameResponse(GameCreationResult result,
                                                           GameMode gameMode,
                                                           PlayerMatchType matchType) {
        MultiRoadViewGame game = result.getGame();
        List<GamePlayerResponse> playerResponses = toPlayerResponses(result.getPlayers());

        return MultiGameResponse.StartGame.builder()
                .gameId(game.getId())
                .gameMode(gameMode.name())
                .matchType(matchType.name())
                .totalRounds(game.getTotalRounds())
                .roundId(INITIAL_ROUND_ID)
                .isPoiNameVisible(game.isPoiNameVisible())
                .currentRound(game.getCurrentRound())
                .roundTimeLimit(game.getTimeLimit())
                .players(playerResponses)
                .payload(null)
                .build();
    }

    /**
     * 게임과 플레이어 목록을 StartGame 응답으로 변환한다.
     *
     * @param game      게임 엔티티
     * @param players   플레이어 목록
     * @param gameMode  게임 모드
     * @param matchType 매치 타입
     * @return 게임 시작 응답 DTO
     */
    public MultiGameResponse.StartGame toStartGameResponse(MultiRoadViewGame game,
                                                           List<GamePlayer> players,
                                                           GameMode gameMode,
                                                           PlayerMatchType matchType) {
        List<GamePlayerResponse> playerResponses = toPlayerResponses(players);

        return MultiGameResponse.StartGame.builder()
                .gameId(game.getId())
                .gameMode(gameMode.name())
                .matchType(matchType.name())
                .totalRounds(game.getTotalRounds())
                .roundId(INITIAL_ROUND_ID)
                .isPoiNameVisible(game.isPoiNameVisible())
                .currentRound(game.getCurrentRound())
                .roundTimeLimit(game.getTimeLimit())
                .players(playerResponses)
                .payload(null)
                .build();
    }

    /**
     * GamePlayer 목록을 GamePlayerResponse 목록으로 변환한다.
     *
     * @param players 플레이어 목록
     * @return 플레이어 응답 DTO 목록
     */
    public List<GamePlayerResponse> toPlayerResponses(List<GamePlayer> players) {
        return players.stream()
                .map(GamePlayerResponse::from)
                .toList();
    }

    /**
     * 게임 플레이어 목록을 GameFinalResult 응답으로 변환한다.
     *
     * @param gameId  게임 ID
     * @param players 플레이어 목록
     * @return 게임 최종 결과 응답 DTO
     */
    public MultiGameResponse.GameFinalResult toGameFinalResult(Long gameId, List<GamePlayer> players) {
        return MultiGameResponse.GameFinalResult.from(gameId, players);
    }
}

