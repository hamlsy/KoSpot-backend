package com.kospot.multi.round.application.usecase.roadview;

import com.kospot.game.domain.vo.GameMode;
import com.kospot.multi.game.domain.vo.PlayerMatchType;
import com.kospot.multi.room.infrastructure.redis.adaptor.GameRoomRedisAdaptor;
import com.kospot.multi.round.application.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.multi.round.entity.RoadViewGameRound;
import com.kospot.multi.submission.entity.event.EarlyRoundCompletionEvent;
import com.kospot.multi.submission.application.service.RoadViewSubmissionService;
import com.kospot.common.annotation.usecase.UseCase;

import com.kospot.multi.submission.infrastructure.redis.service.SubmissionRedisService;
import com.kospot.multi.timer.infrastructure.websocket.service.GameTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class CheckAndCompleteRoundEarlyUseCase {

    private final RoadViewGameRoundAdaptor roadViewGameRoundAdaptor;
    private final SubmissionRedisService submissionRedisService;
    private final GameRoomRedisAdaptor gameRoomRedisAdaptor;
    private final RoadViewSubmissionService roadViewSubmissionService;
    private final GameTimerService gameTimerService;
    private final ApplicationEventPublisher eventPublisher;

    public boolean execute(String gameRoomId, Long gameId, Long roundId,
                           GameMode mode, PlayerMatchType matchType) {
        long submissionCount = submissionRedisService.getCurrentSubmissionCount(mode, roundId);
        long expectedCount = getExpectedSubmissionCount(matchType, gameRoomId);
        if (submissionCount < expectedCount) {
            return false;
        }

        // 4. DB 기반 최종 검증 및 조기 종료 실행
//        return completeRoundEarly(gameRoomId, gameId, roundId, mode, matchType);
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryById(roundId);
        round.validateRoundNotFinished();
        gameTimerService.stopRoundTimer(gameRoomId, round);

        log.info("Round completed early - RoundId: {}, MatchType: {}", roundId, matchType);

        EarlyRoundCompletionEvent event = new EarlyRoundCompletionEvent(
                gameRoomId, gameId, roundId, mode, matchType
        );
        eventPublisher.publishEvent(event);
        return true;
    }

    private boolean completeRoundEarly(String gameRoomId, Long gameId, Long roundId,
                                       GameMode mode, PlayerMatchType matchType) {
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryById(roundId);
        round.validateRoundNotFinished();

        boolean allSubmitted = roadViewSubmissionService.hasAllParticipantsSubmitted(
                gameId, roundId, matchType
        );
        
        if (!allSubmitted) {
            log.warn("Redis-DB mismatch detected - RoundId: {}, MatchType: {}",
                    roundId, matchType);
            return false;
        }

        gameTimerService.stopRoundTimer(gameRoomId, round);
        
        log.info("Round completed early - RoundId: {}, MatchType: {}", roundId, matchType);

        EarlyRoundCompletionEvent event = new EarlyRoundCompletionEvent(
                gameRoomId, gameId, roundId, mode, matchType
        );
        eventPublisher.publishEvent(event);

        return true;
    }

    /**
     * 매치 타입에 따른 예상 제출 수 계산
     * - 개인전: Redis에서 현재 플레이어 수 조회
     * - 팀전: Redis에서 팀 수 조회 (향후 구현)
     */
    private long getExpectedSubmissionCount(PlayerMatchType matchType, String gameRoomId) {
        return switch (matchType) {
            case SOLO -> gameRoomRedisAdaptor.getCurrentPlayersCount(gameRoomId);
            case TEAM -> {
                // TODO: Redis에서 팀 수 조회 로직 구현
                log.warn("⚠️ Team mode expected count not implemented yet");
                yield gameRoomRedisAdaptor.getCurrentPlayersCount(gameRoomId);
            }
        };
    }
}
