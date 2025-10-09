package com.kospot.domain.multi.submission.service;

import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.submission.entity.roadView.RoadViewPlayerSubmission;
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
public class RoadViewSoloSubmissionService {

    private final RoadViewPlayerSubmissionRepository roadViewPlayerSubmissionRepository;

    public void createSubmission(RoadViewGameRound round, GamePlayer gamePlayer, RoadViewPlayerSubmission submission) {
        validateSubmissionAllowed(round, gamePlayer.getId());

        submission.setGamePlayer(gamePlayer);
        submission.setRoadViewGameRound(round);
        roadViewPlayerSubmissionRepository.save(submission);
    }

    public List<RoadViewPlayerSubmission> updateRankAndScore(List<RoadViewPlayerSubmission> submissions) {
        submissions.sort(Comparator.comparingDouble(RoadViewPlayerSubmission::getDistance)
                .thenComparingDouble(RoadViewPlayerSubmission::getTimeToAnswer));
        IntStream.range(0, submissions.size()).forEach(
                i -> submissions.get(i).assignRankAndScore(i + 1));
        //game player 점수 계산
        submissions.forEach(submission -> {
            GamePlayer gamePlayer = submission.getGamePlayer();
            gamePlayer.addScore(submission.getEarnedScore());
        });
        return submissions;
    }

    private void validateSubmissionAllowed(RoadViewGameRound round, Long playerId) {
        round.validateRoundNotFinished();
        if (roadViewPlayerSubmissionRepository.existsByRoundIdAndGamePlayerId(round.getId(), playerId)) {
            throw new GameRoundHandler(ErrorStatus.ROUND_ALREADY_SUBMITTED);
        }
    }

}
