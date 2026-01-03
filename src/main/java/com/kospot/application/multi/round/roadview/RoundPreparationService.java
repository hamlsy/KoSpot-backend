package com.kospot.application.multi.round.roadview;

import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.service.roadview.RoadViewGameRoundService;
import com.kospot.infrastructure.exception.object.domain.GameRoundHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 라운드 준비를 담당하는 도메인 서비스
 * 게임 상태 변경, 라운드 생성 등 순수 도메인 로직을 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoundPreparationService {

    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final RoadViewGameRoundAdaptor roadViewGameRoundAdaptor;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GamePlayerAdaptor gamePlayerAdaptor;

    /**
     * 첫 번째 라운드를 준비한다.
     * 게임을 시작 상태로 변경하고 첫 라운드를 생성한다.
     *
     * @param gameId 게임 ID
     * @return 라운드 준비 결과 (게임이 이미 진행 중이면 null)
     */
    public InitialRoundResult prepareInitialRound(Long gameId) {
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(gameId);

        if (game.isInProgress()) {
            log.info("Game already started - skip initial preparation. GameId: {}", gameId);
            return null;
        }

        game.startGame();

        List<GamePlayer> players = gamePlayerAdaptor.queryByMultiRoadViewGameIdWithMember(gameId);
        List<Long> playerIds = players.stream()
                .map(GamePlayer::getId)
                .toList();

        RoadViewGameRound round = roadViewGameRoundService.createGameRound(game, playerIds);

        return new InitialRoundResult(game, round, players);
    }

    /**
     * 다음 라운드를 준비한다.
     * 게임의 현재 라운드를 증가시키고 새 라운드를 생성한다.
     *
     * @param gameId 게임 ID
     * @return 라운드 준비 결과
     */
    public NextRoundResult prepareNextRound(Long gameId) {
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(gameId);
        game.moveToNextRound();

        RoadViewGameRound round = roadViewGameRoundService.createGameRound(game, null);
        return new NextRoundResult(game, round);
    }

    /**
     * 라운드를 재발행한다.
     * 기존 좌표를 무효화하고 새 좌표로 교체한다.
     *
     * @param roundId 라운드 ID
     * @param gameId  게임 ID (검증용)
     * @return 재발행 결과
     */
    public ReissueResult reissueRound(Long roundId, Long gameId) {
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryByIdFetchGame(roundId);
        MultiRoadViewGame game = round.getMultiRoadViewGame();

        if (!game.getId().equals(gameId)) {
            throw new GameRoundHandler(ErrorStatus.GAME_ROUND_NOT_FOUND);
        }

        roadViewGameRoundService.reissueRound(round, round.getPlayerIds());
        return new ReissueResult(game, round);
    }

    /**
     * 라운드를 시작 상태로 변경한다.
     *
     * @param roundId 라운드 ID
     * @return 라운드 엔티티
     */
    public RoadViewGameRound startRound(Long roundId) {
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryById(roundId);
        round.startRound();
        return round;
    }

    // === Result DTOs ===

    @Getter
    @RequiredArgsConstructor
    public static class InitialRoundResult {
        private final MultiRoadViewGame game;
        private final RoadViewGameRound round;
        private final List<GamePlayer> players;
    }

    @Getter
    @RequiredArgsConstructor
    public static class NextRoundResult {
        private final MultiRoadViewGame game;
        private final RoadViewGameRound round;
    }

    @Getter
    @RequiredArgsConstructor
    public static class ReissueResult {
        private final MultiRoadViewGame game;
        private final RoadViewGameRound round;
    }
}

