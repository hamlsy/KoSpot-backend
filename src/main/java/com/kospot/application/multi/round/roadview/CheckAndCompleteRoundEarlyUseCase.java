package com.kospot.application.multi.round.roadview;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.round.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.submission.service.RoadViewSubmissionService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.multi.room.adaptor.GameRoomRedisAdaptor;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.infrastructure.redis.domain.multi.submission.service.SubmissionRedisService;
import com.kospot.infrastructure.websocket.domain.multi.timer.service.GameTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class CheckAndCompleteRoundEarlyUseCase {

    private final RoadViewGameRoundAdaptor roadViewGameRoundAdaptor;
    private final SubmissionRedisService submissionRedisService;
    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomRedisAdaptor gameRoomRedisAdaptor;
    private final RoadViewSubmissionService roadViewSubmissionService;
    private final GameTimerService gameTimerService;

    public boolean execute(GameMode mode, String gameRoomId, Long gameId, Long roundId) {
        // 팀모드, 개인모드 구분
        // 우선 개인모드만
        // 1. redis에서 제출 수 확인
        long submissionCount = submissionRedisService.getCurrentSubmissionCount(mode, roundId);

        // 2. 게임 방 플레이어 수 조회
        Long playerCount = gameRoomRedisAdaptor.getCurrentPlayers(gameRoomId);

        // 3. 모든 플레이어가 제출하지 않았으면 false 리턴
        if(submissionCount < playerCount) {
            return false;
        }

        // 4. 동시성 제어 todo implement
        return completeRoadViewRoundEarly(gameRoomId, gameId, roundId, playerCount);
    }

    // 라운드 조기 종료 실행
    private boolean completeRoadViewRoundEarly(String gameRoomId, Long gameId, Long roundId, long playerCount) {
        //1. 라운드 상태 확인
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryByIdFetchSubmissions(roundId);
        round.validateRoundNotFinished();

        // 2. DB기반 최종 점검
        boolean allSubmitted = roadViewSubmissionService.hasAllParticipantsSubmitted(roundId, PlayerMatchType.SOLO, (int) playerCount);
        if(!allSubmitted) {
            return false;
        }

        // 3. 타이머 중지
        gameTimerService.stopRoundTimer(gameRoomId, round);

        // 4. 조기 종료 이벤트 발행
        //
        //eventPublisher.publishEvent();

        return true;
    }
}
