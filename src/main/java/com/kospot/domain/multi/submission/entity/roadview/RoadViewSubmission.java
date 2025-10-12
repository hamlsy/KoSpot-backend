package com.kospot.domain.multi.submission.entity.roadview;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.multi.game.util.ScoreRule;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import jakarta.persistence.*;
import lombok.*;

/**
 * 로드뷰 제출 엔티티 (개인전 + 팀전 통합)
 * 
 * 설계 철학:
 * - 개인전과 팀전의 공통점: 90% 이상
 * - 차이점: "누가 제출했는가" (플레이어 vs 팀)
 * - 해결: 다형성 연관관계 + 매치타입 구분
 * 
 * 클린코드 원칙:
 * - 단일 책임: 제출 데이터 관리
 * - DRY: 중복 코드 90% 제거
 * - 다형성: matchType으로 동작 분기
 * - 불변성: Setter 최소화, Builder 패턴
 * 
 * 실무 경험:
 * - 분리된 엔티티: 초기엔 깔끔해 보이지만 유지보수 지옥
 * - 통합 엔티티: 초기엔 복잡해 보이지만 장기적으로 훨씬 관리 용이
 */
@Entity
@Table(
        name = "road_view_submission",
        indexes = {
                @Index(name = "idx_round_match_type", columnList = "game_round_id, match_type"),
                @Index(name = "idx_round_player", columnList = "game_round_id, game_player_id"),
                @Index(name = "idx_round_team", columnList = "game_round_id, team_number")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RoadViewSubmission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 20)
    private PlayerMatchType matchType;

    // === 다형성 연관관계 ===

    /**
     * 개인전: 제출한 플레이어
     * - matchType = SOLO일 때만 not null
     * - matchType = TEAM일 때는 null
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_player_id")
    private GamePlayer gamePlayer;

    /**
     * 팀전: 팀 번호 (1, 2, 3, 4)
     * - matchType = TEAM일 때만 not null
     * - matchType = SOLO일 때는 null
     */
    @Column(name = "team_number")
    private Integer teamNumber;

    // === 공통 필드 (90%) ===

    /**
     * 라운드 참조
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_round_id", nullable = false)
    private RoadViewGameRound round;

    /**
     * 제출 위도
     */
    @Column(nullable = false)
    private Double lat;

    /**
     * 제출 경도
     */
    @Column(nullable = false)
    private Double lng;

    /**
     * 정답과의 거리 (미터 단위)
     * - 프론트엔드에서 계산된 값
     */
    @Column(nullable = false)
    private Double distance;

    /**
     * 정답까지 걸린 시간 (밀리초 단위)
     * - 동점 처리용
     */
    @Column(name = "time_to_answer", nullable = false)
    private Double timeToAnswer;

    /**
     * 획득 점수
     */
    @Column(name = "earned_score")
    private Integer earnedScore;

    /**
     * 순위 (캐시용, 조회 성능 향상)
     */
    @Column(name = "rank")
    private Integer rank;

    // === 비즈니스 로직 ===

    /**
     * 라운드 설정 및 양방향 연관관계 설정
     */
    public void setRound(RoadViewGameRound round) {
        this.round = round;
        round.addSubmission(this);
    }

    /**
     * 제출자 ID 조회 (다형성 처리)
     * 
     * 장점: 클라이언트 코드가 개인전/팀전 구분 불필요
     */
    public Long getSubmitterId() {
        return switch (matchType) {
            case SOLO -> gamePlayer != null ? gamePlayer.getId() : null;
            case TEAM -> teamNumber != null ? teamNumber.longValue() : null;
        };
    }

    /**
     * 제출자 이름 조회 (다형성 처리)
     */
    public String getSubmitterName() {
        return switch (matchType) {
            case SOLO -> gamePlayer != null ? gamePlayer.getMember().getNickname() : "Unknown";
            case TEAM -> teamNumber != null ? "Team " + teamNumber : "Unknown";
        };
    }

    /**
     * 점수 및 순위 설정
     */
    public void assignRankAndScore(int rank, int score) {
        this.rank = rank;
        this.earnedScore = score;
    }

    /**
     * 개인전 점수 부여 (ScoreRule 사용)
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

    /**
     * 개인전 여부 확인
     */
    public boolean isSoloMode() {
        return matchType == PlayerMatchType.SOLO;
    }

    /**
     * 팀전 여부 확인
     */
    public boolean isTeamMode() {
        return matchType == PlayerMatchType.TEAM;
    }

    // === 정적 팩토리 메서드 ===

    /**
     * 개인전 제출 생성
     * 
     * 장점: 
     * - 매개변수 의미가 명확
     * - 불변 객체 생성
     * - null 안전성 보장
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
                0.0,  // 미제출 좌표
                0.0,
                Double.MAX_VALUE,  // 최대 거리 (최하위 순위)
                round.getDuration().toMillis()  // 전체 시간 소모
        );
    }

    /**
     * 미제출 팀 0점 생성 (팀전)
     */
    public static RoadViewSubmission zeroForTeam(Integer teamNumber, RoadViewGameRound round) {
        return forTeam(
                teamNumber,
                round,
                0.0,
                0.0,
                Double.MAX_VALUE,
                round.getDuration().toMillis()
        );
    }

    // === Validation 메서드 (클린코드: Guard Clause) ===

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

