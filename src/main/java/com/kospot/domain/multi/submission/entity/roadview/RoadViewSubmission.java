package com.kospot.domain.multi.submission.entity.roadview;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.multi.game.util.ScoreRule;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;


@Entity
@Table(
        name = "road_view_submission",
        indexes = {
                @Index(name = "idx_round_match_type", columnList = "game_round_id, match_type"),
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class RoadViewSubmission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 20)
    private PlayerMatchType matchType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_player_id")
    private GamePlayer gamePlayer;

    /**
     * 팀전: 팀 번호 (1, 2, 3, 4)
     */
    @Column(name = "team_number")
    private Integer teamNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_round_id", nullable = false)
    private RoadViewGameRound round;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(nullable = false)
    private Double distance;

    @Column(name = "time_to_answer", nullable = false)
    private Double timeToAnswer;

    @Column(name = "earned_score")
    private Integer earnedScore;

    @Column(name = "rank")
    private Integer rank;

    // business
    public void setRound(RoadViewGameRound round) {
        this.round = round;
        round.addSubmission(this);
    }

    public Long getSubmitterId() {
        return switch (matchType) {
            case SOLO -> gamePlayer != null ? gamePlayer.getId() : null;
            case TEAM -> teamNumber != null ? teamNumber.longValue() : null;
        };
    }

    public void assignRankAndScore(int rank, int score) {
        this.rank = rank;
        this.earnedScore = score;
    }

    /**
     * 개인전 점수 부여 - todo refactoring
     */
    public void assignPlayerScore(int rank) {
        this.rank = rank;
        this.earnedScore = ScoreRule.calculateScore(rank);
    }

    /**
     * 팀전 점수 부여 (고정 점수)
     * 1등: 10점, 2등: 6점, 3등: 2점, 4등 이상: 0점
     */
    public void assignTeamScore(int rank) {
        this.rank = rank;
        this.earnedScore = switch (rank) {
            case 1 -> 10;
            case 2 -> 6;
            case 3 -> 2;
            default -> 0;
        };
    }

    public boolean isSoloMode() {
        return matchType == PlayerMatchType.SOLO;
    }

    public boolean isTeamMode() {
        return matchType == PlayerMatchType.TEAM;
    }

    // === 정적 팩토리 메서드 ===

    /**
     * 개인전 제출 생성
     */
    public static RoadViewSubmission forPlayer(
            GamePlayer player,
            RoadViewGameRound round,
            Double lat,
            Double lng,
            Double distance,
            Double timeToAnswer
    ) {
        validateNotNull(player, "GamePlayer cannot be null");
        validateNotNull(round, "RoadViewGameRound cannot be null");
        validateCoordinates(lat, lng);
        validateDistance(distance);
        validateTime(timeToAnswer);

        return RoadViewSubmission.builder()
                .matchType(PlayerMatchType.SOLO)
                .gamePlayer(player)
                .teamNumber(null)  // 명시적 null
                .round(round)
                .lat(lat)
                .lng(lng)
                .distance(distance)
                .timeToAnswer(timeToAnswer)
                .build();
    }

    /**
     * 팀전 제출 생성
     */
    public static RoadViewSubmission forTeam(
            Integer teamNumber,
            RoadViewGameRound round,
            Double lat,
            Double lng,
            Double distance,
            Double timeToAnswer
    ) {
        validateNotNull(teamNumber, "Team number cannot be null");
        validateNotNull(round, "RoadViewGameRound cannot be null");
        validateTeamNumber(teamNumber);
        validateCoordinates(lat, lng);
        validateDistance(distance);
        validateTime(timeToAnswer);

        return RoadViewSubmission.builder()
                .matchType(PlayerMatchType.TEAM)
                .gamePlayer(null)  // 명시적 null
                .teamNumber(teamNumber)
                .round(round)
                .lat(lat)
                .lng(lng)
                .distance(distance)
                .timeToAnswer(timeToAnswer)
                .build();
    }

    /**
     * 미제출자 0점 생성 (개인전)
     */
    public static RoadViewSubmission zeroForPlayer(GamePlayer player, RoadViewGameRound round) {
        return forPlayer(
                player,
                round,
                null,  // 미제출 좌표
                null,
                Double.MAX_VALUE,  // 최대 거리 (최하위 순위)
                (double) round.getDuration().toMillis()  // 전체 시간 소모
        );
    }

    /**
     * 미제출 팀 0점 생성 (팀전)
     */
    public static RoadViewSubmission zeroForTeam(Integer teamNumber, RoadViewGameRound round) {
        return forTeam(
                teamNumber,
                round,
                null,
                null,
                Double.MAX_VALUE,
                (double) round.getDuration().toMillis()
        );
    }

    // Validation

    private static void validateNotNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void validateCoordinates(Double lat, Double lng) {
        if (lat == null || lng == null) {
            throw new IllegalArgumentException("Coordinates cannot be null");
        }
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (lng < -180 || lng > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    private static void validateDistance(Double distance) {
        if (distance == null || distance < 0) {
            throw new IllegalArgumentException("Distance must be non-negative");
        }
    }

    private static void validateTime(Double timeToAnswer) {
        if (timeToAnswer == null || timeToAnswer < 0) {
            throw new IllegalArgumentException("Time to answer must be non-negative");
        }
    }

    private static void validateTeamNumber(Integer teamNumber) {
        if (teamNumber < 1 || teamNumber > 4) {
            throw new IllegalArgumentException("Team number must be between 1 and 4");
        }
    }
}

