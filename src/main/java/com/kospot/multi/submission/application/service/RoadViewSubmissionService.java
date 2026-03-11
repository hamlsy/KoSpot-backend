package com.kospot.multi.submission.application.service;

import com.kospot.multi.game.domain.vo.PlayerMatchType;
import com.kospot.multi.player.application.adaptor.GamePlayerAdaptor;
import com.kospot.multi.player.domain.entity.GamePlayer;
import com.kospot.multi.player.domain.vo.GamePlayerStatus;
import com.kospot.multi.round.entity.RoadViewGameRound;
import com.kospot.multi.submission.entity.roadview.RoadViewSubmission;
import com.kospot.multi.submission.infrastructure.persistence.RoadViewSubmissionRepository;
import com.kospot.common.exception.object.domain.GameRoundHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
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
                round.getId(),
                submission.getLat(),
                submission.getLng(),
                submission.getTimeToAnswer()
        );
        newSubmission.assignDistanceAndPlayerScore(round.getTargetCoordinate());
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
                round.getId(),
                submission.getLat(),
                submission.getLng(),
                submission.getDistance(),
                submission.getTimeToAnswer()
        );

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

        // 2. 순위 부여
        for (int i = 0; i < sorted.size(); i++) {
            int rank = i + 1;
            RoadViewSubmission submission = sorted.get(i);
            
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

            // 3. 팀원들에게 점수 분배
            distributeScoreToTeamMembers(gameId, submission);
        }

        return sorted;
    }


    private void distributeScoreToTeamMembers(Long gameId, RoadViewSubmission submission) {
        if (submission.getTeamNumber() == null) {
            return;
        }

        Integer teamNumber = submission.getTeamNumber();
        double teamScore = submission.getEarnedScore();

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

    /**
     * 모든 참가자(플레이어 또는 팀)의 제출 완료 여부 확인
     */
    public boolean hasAllParticipantsSubmitted(Long gameId, Long roundId, PlayerMatchType matchType) {
        long submissionCount = repository.countByRoundIdAndMatchType(roundId, matchType);
        int expectedCount = getExpectedSubmissionCount(gameId, matchType);
        log.info("📝 Submission count check - RoundId: {}, MatchType: {}, Submitted: {}, Expected: {}",
                roundId, matchType, submissionCount, expectedCount);
        return submissionCount >= expectedCount;
    }

    public boolean hasAllPlayersSubmitted(Long gameId, Long roundId) {
        long submissionCount = repository.countByRoundIdAndMatchType(roundId, PlayerMatchType.SOLO);
        int totalPlayers = gamePlayerAdaptor.countPlayersByRoadViewGameId(gameId);
        return submissionCount >= totalPlayers;
    }

    public boolean hasAllTeamsSubmitted(Long gameId, Long roundId) {
        long submissionCount = repository.countByRoundIdAndMatchType(roundId, PlayerMatchType.TEAM);
        int totalTeams = gamePlayerAdaptor.countTeamsByGameId(gameId);
        return submissionCount >= totalTeams;
    }

    private int getExpectedSubmissionCount(Long gameId, PlayerMatchType matchType) {
        return switch (matchType) {
            case SOLO -> gamePlayerAdaptor.countPlayersByRoadViewGameIdAndStatus(gameId, GamePlayerStatus.PLAYING);
            case TEAM -> gamePlayerAdaptor.countTeamsByGameId(gameId); //todo refactoring
        };
    }

    public List<RoadViewSubmission> getSubmissionsByRoundOrderByDistance(
            Long roundId,
            PlayerMatchType matchType
    ) {
        return switch (matchType) {
            case SOLO -> repository.findSoloSubmissionsByRoundIdOrderByDistance(roundId);
            case TEAM -> repository.findTeamSubmissionsByRoundIdOrderByDistance(roundId);
        };
    }


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
        Double maxTime = (double) round.getDuration().toMillis();
        List<RoadViewSubmission> zeroSubmissions = nonSubmittedPlayerIds.stream()
                .map(playerId -> {
                    GamePlayer player = gamePlayerAdaptor.queryById(playerId);
                    RoadViewSubmission zeroSubmission = RoadViewSubmission.zeroForPlayer(
                            player, 
                            round.getId(),
                            maxTime
                    );
                    
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
        Double maxTime = (double) round.getDuration().toMillis();
        List<RoadViewSubmission> zeroSubmissions = nonSubmittedTeamNumbers.stream()
                .map(teamNumber -> {
                    RoadViewSubmission zeroSubmission = RoadViewSubmission.zeroForTeam(
                            teamNumber, 
                            round.getId(),
                            maxTime
                    );
                    
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

