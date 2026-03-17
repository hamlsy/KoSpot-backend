package com.kospot.multi.round.application.service.roadview;

import com.kospot.multi.game.application.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.multi.game.domain.entity.MultiRoadViewGame;
import com.kospot.multi.player.application.adaptor.GamePlayerAdaptor;
import com.kospot.multi.player.domain.entity.GamePlayer;
import com.kospot.multi.round.application.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.multi.round.entity.RoadViewGameRound;
import com.kospot.multi.round.application.roadview.RoadViewGameRoundService;
import com.kospot.common.exception.object.domain.GameRoundHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
        boolean transitioned = multiRoadViewGameAdaptor.transitionToInProgressIfPending(gameId);
        if (!transitioned) {
            log.info("Game already started or not pending - skip initial preparation. GameId: {}", gameId);
            return null;
        }

        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(gameId);

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
        return reissueRound(round, gameId);
    }

    public RoadViewGameRound getRoundForReissueWithLock(Long roundId, Long gameId) {
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryByIdFetchGameForUpdate(roundId);
        validateGameMatch(round, gameId);
        return round;
    }

    public RoadViewGameRound getRoundForReissue(Long roundId, Long gameId, Long roomId) {
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryByIdFetchCoordinateAndGame(roundId);
        validateOwnership(round, gameId, roomId);
        return round;
    }

    public int tryAdvanceReissueVersion(Long roundId,
                                        Long gameId,
                                        Long expectedVersion,
                                        Integer maxReissueCount,
                                        Instant cooldownThreshold,
                                        Instant now) {
        return roadViewGameRoundAdaptor.tryAdvanceReissueVersion(
                roundId,
                gameId,
                expectedVersion,
                maxReissueCount,
                cooldownThreshold,
                now
        );
    }

    public ReissueResult reissueRound(RoadViewGameRound round, Long gameId) {
        MultiRoadViewGame game = validateGameMatch(round, gameId);
        roadViewGameRoundService.reissueRound(round, round.getPlayerIds());
        return new ReissueResult(game, round);
    }

    public void validateOwnership(RoadViewGameRound round, Long gameId, Long roomId) {
        MultiRoadViewGame game = validateGameMatch(round, gameId);
        if (!game.getGameRoomId().equals(roomId)) {
            throw new GameRoundHandler(ErrorStatus.GAME_ROUND_NOT_FOUND);
        }
    }

    private MultiRoadViewGame validateGameMatch(RoadViewGameRound round, Long gameId) {
        MultiRoadViewGame game = round.getMultiRoadViewGame();

        if (!game.getId().equals(gameId)) {
            throw new GameRoundHandler(ErrorStatus.GAME_ROUND_NOT_FOUND);
        }

        return game;
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

