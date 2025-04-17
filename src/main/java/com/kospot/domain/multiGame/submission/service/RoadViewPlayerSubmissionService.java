package com.kospot.domain.multiGame.submission.service;

import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multiGame.submission.entity.roadView.RoadViewPlayerSubmission;
import com.kospot.domain.multiGame.submission.repository.RoadViewPlayerSubmissionRepository;
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

    private final RoadViewPlayerSubmissionRepository roadViewPlayerSubmissionRepository;

    public void createSubmission(RoadViewGameRound roadViewGameRound, GamePlayer gamePlayer, RoadViewPlayerSubmission submission) {
        submission.setGamePlayer(gamePlayer);
        submission.setRoadViewGameRound(roadViewGameRound);
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
            gamePlayer.addScore(submission.getScore());
        });
        return submissions;
    }

}
