package com.kospot.domain.multi.submission.service;

import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.entity.BaseGameRound;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.submission.entity.roadview.RoadViewPlayerSubmission;
import com.kospot.domain.multi.submission.repository.RoadViewPlayerSubmissionRepository;

import com.kospot.infrastructure.exception.object.domain.GameRoundHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoadViewPlayerSubmissionService {

    private final RoadViewPlayerSubmissionAdaptor
    private final RoadViewPlayerSubmissionRepository roadViewPlayerSubmissionRepository;

    public void createSubmission(RoadViewGameRound round, GamePlayer gamePlayer,
                                 RoadViewPlayerSubmission submission) {
        validateSubmissionAllowed(round, gamePlayer.getId());

        submission.setGamePlayer(gamePlayer);
        submission.setRound(round);
        roadViewPlayerSubmissionRepository.save(submission);
    }

    //todo 거리 기반 점수로 변경
    //todo 시간 순 추가 점수
    public List<RoadViewPlayerSubmission> updateRankAndScore(List<RoadViewPlayerSubmission> submissions) {
        submissions.sort(Comparator.comparingDouble(RoadViewPlayerSubmission::getDistance)
                .thenComparingDouble(RoadViewPlayerSubmission::getTimeToAnswer));
        IntStream.range(0, submissions.size()).forEach(
                i -> submissions.get(i).assignScore(i + 1));
        //game player 점수 계산
        submissions.forEach(submission -> {
            GamePlayer gamePlayer = submission.getGamePlayer();
            gamePlayer.addScore(submission.getEarnedScore());
        });
        return submissions;
    }

    public boolean hasAllPlayersSubmitted(Long roundId, int totalPlayers) {
        long submissionCount =
    }

    private void validateSubmissionAllowed(RoadViewGameRound round, Long playerId) {
        round.validateRoundNotFinished();
        // 중복 제출 검증
        validateNotAlreadySubmitted(round, playerId);
    }

    private void validateNotAlreadySubmitted(RoadViewGameRound round, Long playerId) {
        if (roadViewPlayerSubmissionRepository.existsByRoundIdAndGamePlayerId(round.getId(), playerId)) {
            throw new GameRoundHandler(ErrorStatus.ROUND_ALREADY_SUBMITTED);
        }
    }

    private void validateTimeLimit(BaseGameRound round) {
        if (round.isTimeLimitExceeded()) {
            throw new GameRoundHandler(ErrorStatus.ROUND_TIME_LIMIT_EXCEEDED);
        }
    }

}
