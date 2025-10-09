package com.kospot.domain.multi.submission.service;

import com.kospot.domain.multi.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.submission.entity.roadView.RoadViewTeamSubmission;
import com.kospot.domain.multi.submission.repository.RoadViewTeamSubmissionRepository;
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
public class RoadViewTeamSubmissionService {

    private final RoadViewTeamSubmissionRepository roadViewTeamSubmissionRepository;
    private final GamePlayerAdaptor gamePlayerAdaptor;

    /**
     * 팀 제출 생성
     */
    public void createSubmission(
            RoadViewGameRound round,
            Integer teamNumber,
            RoadViewTeamSubmission submission
    ) {
        validateTeamSubmissionAllowed(round, teamNumber);
        submission.setRound(round);
        roadViewTeamSubmissionRepository.save(submission);
    }

    /**
     * 팀 순위 계산 및 점수 부여
     * - 거리 순으로 정렬
     * - 순위별 점수 부여 (1등 10점, 2등 6점, 3등 2점, 4등 이상 0점)
     * - 팀원 모두에게 점수 분배
     */
    public List<RoadViewTeamSubmission> calculateRankAndScore(
            List<RoadViewTeamSubmission> submissions,
            Long gameId
    ) {
        // 1. 거리 순 정렬 (동일 거리면 시간 순)
        submissions.sort(
                Comparator.comparingDouble(RoadViewTeamSubmission::getDistance)
                        .thenComparingDouble(RoadViewTeamSubmission::getTimeToAnswer)
        );

        // 2. 순위 부여 및 점수 계산
        IntStream.range(0, submissions.size()).forEach(i -> {
            RoadViewTeamSubmission submission = submissions.get(i);
            Integer rank = i + 1;
            submission.assignScore(rank);

            // 3. 팀원들에게 점수 분배
            distributeScoreToTeamMembers(gameId, submission);
        });

        return submissions;
    }

    /**
     * 팀 점수를 팀원 모두에게 분배
     */
    private void distributeScoreToTeamMembers(Long gameId, RoadViewTeamSubmission submission) {
        Integer teamNumber = submission.getTeamNumber();
        Integer teamScore = submission.getEarnedScore();

        // 해당 팀의 모든 플레이어 조회
        List<GamePlayer> teamMembers = gamePlayerAdaptor.queryByGameIdAndTeamNumber(gameId, teamNumber);

        // 각 팀원에게 점수 분배
        teamMembers.forEach(player -> {
            player.addScore(teamScore);
            log.debug("팀 점수 분배 - Player: {}, Team: {}, Score: {}", 
                    player.getId(), teamNumber, teamScore);
        });
    }

    /**
     * 팀 제출 가능 여부 검증
     */
    private void validateTeamSubmissionAllowed(RoadViewGameRound round, Integer teamNumber) {
        round.validateRoundNotFinished();

        if (roadViewTeamSubmissionRepository.existsByRoundIdAndTeamNumber(round.getId(), teamNumber)) {
            throw new GameRoundHandler(ErrorStatus.ROUND_ALREADY_SUBMITTED);
        }
    }
}

