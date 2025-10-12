package com.kospot.domain.multi.submission.service;

import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.submission.entity.roadview.RoadViewSubmission;
import com.kospot.domain.multi.submission.repository.RoadViewSubmissionRepository;
import com.kospot.infrastructure.exception.object.domain.GameRoundHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoadViewSubmissionService {

    private final RoadViewSubmissionRepository repository;
    private final GamePlayerAdaptor gamePlayerAdaptor;

    public RoadViewSubmission createPlayerSubmission(
            RoadViewGameRound round,
            GamePlayer player,
            RoadViewSubmission submission
    ) {
        validatePlayerSubmissionAllowed(round, player.getId());

        RoadViewSubmission newSubmission = RoadViewSubmission.forPlayer(
                player,
                round,
                submission.getLat(),
                submission.getLng(),
                submission.getDistance(),
                submission.getTimeToAnswer()
        );

        newSubmission.setRound(round);
        return repository.save(newSubmission);
    }

    public RoadViewSubmission createTeamSubmission(
            RoadViewGameRound round,
            Integer teamNumber,
            RoadViewSubmission submission
    ) {
        validateTeamSubmissionAllowed(round, teamNumber);
        RoadViewSubmission newSubmission = RoadViewSubmission.forTeam(
                teamNumber,
                round,
                submission.getLat(),
                submission.getLng(),
                submission.getDistance(),
                submission.getTimeToAnswer()
        );

        newSubmission.setRound(round);

        return repository.save(newSubmission);
    }

    public List<RoadViewSubmission> calculatePlayerRankAndScore(List<RoadViewSubmission> submissions) {
        if (submissions.isEmpty()) {
            return submissions;
        }

        // 1. 거리 순 정렬 (동일 거리면 시간 순)
        List<RoadViewSubmission> sorted = submissions.stream()
                .sorted(Comparator.comparingDouble(RoadViewSubmission::getDistance)
                        .thenComparingDouble(RoadViewSubmission::getTimeToAnswer))
                .toList();

        // 2. 순위 부여 및 점수 계산
        for (int i = 0; i < sorted.size(); i++) {
            int rank = i + 1;
            RoadViewSubmission submission = sorted.get(i);
            
            // 개인전 점수 계산 (ScoreRule 사용)
            submission.assignPlayerScore(rank);
            
            // 3. 플레이어에게 점수 부여
            if (submission.getGamePlayer() != null) {
                submission.getGamePlayer().addScore(submission.getEarnedScore());
            }
        }

        return sorted;
    }

    public List<RoadViewSubmission> calculateTeamRankAndScore(
            List<RoadViewSubmission> submissions,
            Long gameId
    ) {
        if (submissions.isEmpty()) {
            return submissions;
        }

        // 1. 거리 순 정렬
        List<RoadViewSubmission> sorted = submissions.stream()
                .sorted(Comparator.comparingDouble(RoadViewSubmission::getDistance)
                        .thenComparingDouble(RoadViewSubmission::getTimeToAnswer))
                .toList();

        // 2. 순위 부여 및 점수 계산
        for (int i = 0; i < sorted.size(); i++) {
            int rank = i + 1;
            RoadViewSubmission submission = sorted.get(i);
            
            // 팀전 점수 계산
            submission.assignTeamScore(rank);
            
            // 3. 팀원들에게 점수 분배
            distributeScoreToTeamMembers(gameId, submission);
            
            log.debug("📊 Team score assigned - Rank: {}, TeamNumber: {}, Distance: {}m, Score: {}",
                    rank, submission.getTeamNumber(), 
                    submission.getDistance(), submission.getEarnedScore());
        }

        return sorted;
    }


    private void distributeScoreToTeamMembers(Long gameId, RoadViewSubmission submission) {
        if (submission.getTeamNumber() == null) {
            return;
        }

        Integer teamNumber = submission.getTeamNumber();
        Integer teamScore = submission.getEarnedScore();

        // 해당 팀의 모든 플레이어 조회
        List<GamePlayer> teamMembers = gamePlayerAdaptor.queryByGameIdAndTeamNumber(gameId, teamNumber);

        // 각 팀원에게 점수 분배
        teamMembers.forEach(player -> {
            player.addScore(teamScore);
            log.debug("💰 Score distributed - PlayerId: {}, TeamNumber: {}, Score: {}",
                    player.getId(), teamNumber, teamScore);
        });
    }

    // === 조회 메서드 ===
    public boolean hasAllParticipantsSubmitted(
            Long roundId,
            PlayerMatchType matchType,
            int totalParticipants
    ) {
        long submissionCount = repository.countByRoundIdAndMatchType(roundId, matchType);
        boolean allSubmitted = submissionCount == totalParticipants;

        log.debug("📊 Submission check - RoundId: {}, Type: {}, Submitted: {}/{}, AllSubmitted: {}",
                roundId, matchType, submissionCount, totalParticipants, allSubmitted);

        return allSubmitted;
    }

    /**
     * 라운드별 제출 목록 조회 (거리 순)
     */
    public List<RoadViewSubmission> getSubmissionsByRoundOrderByDistance(
            Long roundId,
            PlayerMatchType matchType
    ) {
        return switch (matchType) {
            case SOLO -> repository.findSoloSubmissionsByRoundIdOrderByDistance(roundId);
            case TEAM -> repository.findTeamSubmissionsByRoundIdOrderByDistance(roundId);
        };
    }

    /**
     * 라운드별 제출 목록 조회 (순위 순)
     */
    public List<RoadViewSubmission> getSubmissionsByRoundOrderByRank(
            Long roundId,
            PlayerMatchType matchType
    ) {
        return switch (matchType) {
            case SOLO -> repository.findSoloSubmissionsByRoundIdOrderByRank(roundId);
            case TEAM -> repository.findTeamSubmissionsByRoundIdOrderByRank(roundId);
        };
    }

    // === 미제출자 처리 ===

    /**
     * 미제출 플레이어 0점 처리 (개인전)
     */
    public List<RoadViewSubmission> handleNonSubmittedPlayers(
            Long gameId,
            RoadViewGameRound round
    ) {
        // 1. 미제출 플레이어 ID 조회
        List<Long> nonSubmittedPlayerIds = repository.findNonSubmittedPlayerIds(gameId, round.getId());

        if (nonSubmittedPlayerIds.isEmpty()) {
            log.debug("✅ All players submitted - RoundId: {}", round.getId());
            return List.of();
        }

        // 2. 각 미제출 플레이어에 대해 0점 제출 생성
        List<RoadViewSubmission> zeroSubmissions = nonSubmittedPlayerIds.stream()
                .map(playerId -> {
                    GamePlayer player = gamePlayerAdaptor.queryById(playerId);
                    RoadViewSubmission zeroSubmission = RoadViewSubmission.zeroForPlayer(player, round);
                    zeroSubmission.setRound(round);
                    
                    RoadViewSubmission saved = repository.save(zeroSubmission);
                    log.debug("📝 Zero submission created - RoundId: {}, PlayerId: {}",
                            round.getId(), playerId);
                    
                    return saved;
                })
                .toList();

        log.info("✅ Non-submitted players handled - RoundId: {}, Count: {}",
                round.getId(), zeroSubmissions.size());

        return zeroSubmissions;
    }

    /**
     * 미제출 팀 0점 처리 (팀전)
     */
    public List<RoadViewSubmission> handleNonSubmittedTeams(
            Long gameId,
            RoadViewGameRound round
    ) {
        // 1. 미제출 팀 번호 조회
        List<Integer> nonSubmittedTeamNumbers = repository.findNonSubmittedTeamNumbers(gameId, round.getId());

        if (nonSubmittedTeamNumbers.isEmpty()) {
            log.debug("✅ All teams submitted - RoundId: {}", round.getId());
            return List.of();
        }

        // 2. 각 미제출 팀에 대해 0점 제출 생성
        List<RoadViewSubmission> zeroSubmissions = nonSubmittedTeamNumbers.stream()
                .map(teamNumber -> {
                    RoadViewSubmission zeroSubmission = RoadViewSubmission.zeroForTeam(teamNumber, round);
                    zeroSubmission.setRound(round);
                    
                    RoadViewSubmission saved = repository.save(zeroSubmission);
                    log.debug("📝 Zero submission created - RoundId: {}, TeamNumber: {}",
                            round.getId(), teamNumber);
                    
                    return saved;
                })
                .toList();

        return zeroSubmissions;
    }

    //Validation

    /**
     * 개인전 제출 가능 여부 검증
     */
    private void validatePlayerSubmissionAllowed(RoadViewGameRound round, Long playerId) {
        // 1. 라운드 종료 여부 확인
        round.validateRoundNotFinished();

        // 2. 중복 제출 검증
        if (repository.existsByRoundIdAndPlayerId(round.getId(), playerId)) {
            log.warn("⚠️ Duplicate submission attempt - RoundId: {}, PlayerId: {}",
                    round.getId(), playerId);
            throw new GameRoundHandler(ErrorStatus.ROUND_ALREADY_SUBMITTED);
        }
    }

    /**
     * 팀전 제출 가능 여부 검증
     */
    private void validateTeamSubmissionAllowed(RoadViewGameRound round, Integer teamNumber) {
        // 1. 라운드 종료 여부 확인
        round.validateRoundNotFinished();

        // 2. 중복 제출 검증
        if (repository.existsByRoundIdAndTeamNumber(round.getId(), teamNumber)) {
            log.warn("⚠️ Duplicate submission attempt - RoundId: {}, TeamNumber: {}",
                    round.getId(), teamNumber);
            throw new GameRoundHandler(ErrorStatus.ROUND_ALREADY_SUBMITTED);
        }
    }
}

