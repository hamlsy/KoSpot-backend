package com.kospot.domain.multi.round.entity;

import com.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.submission.entity.roadview.RoadViewSubmission;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 로드뷰 게임 라운드 엔티티
 * 
 * 리팩토링:
 * - roadViewPlayerSubmissions + roadViewTeamSubmissions 통합
 * - 단일 roadViewSubmissions 리스트로 관리
 * - matchType으로 자동 구분
 */
@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadViewGameRound extends BaseGameRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_road_view_game_id")
    private MultiRoadViewGame multiRoadViewGame;
    
    // 라운드에 사용되는 정답 좌표
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinate_id")
    private CoordinateNationwide targetCoordinate;

    /**
     * 통합 제출 리스트 (개인전 + 팀전)
     * - matchType으로 자동 구분
     * - 개인전: gamePlayer not null
     * - 팀전: teamNumber not null
     */
    @Builder.Default
    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RoadViewSubmission> roadViewSubmissions = new ArrayList<>();

    @Override
    public GameMode getGameMode() {
        return GameMode.ROADVIEW;
    }

    // === Business methods ===

    public void setMultiRoadViewGame(MultiRoadViewGame multiRoadViewGame) {
        this.multiRoadViewGame = multiRoadViewGame;
    }

    /**
     * 제출 추가 (통합 메서드)
     * - 개인전/팀전 구분 없이 하나의 메서드로 처리
     */
    public void addSubmission(RoadViewSubmission submission) {
        this.roadViewSubmissions.add(submission);
    }

    /**
     * 개인전 제출만 필터링 (하위 호환성)
     */
    public List<RoadViewSubmission> getPlayerSubmissions() {
        return roadViewSubmissions.stream()
                .filter(RoadViewSubmission::isSoloMode)
                .toList();
    }

    /**
     * 팀전 제출만 필터링 (하위 호환성)
     */
    public List<RoadViewSubmission> getTeamSubmissions() {
        return roadViewSubmissions.stream()
                .filter(RoadViewSubmission::isTeamMode)
                .toList();
    }

    /**
     * @deprecated Use addSubmission() instead
     * 하위 호환성을 위해 유지
     */
    @Deprecated(since = "2024-10", forRemoval = true)
    public void addPlayerSubmission(RoadViewSubmission submission) {
        addSubmission(submission);
    }

    /**
     * @deprecated Use addSubmission() instead
     * 하위 호환성을 위해 유지
     */
    @Deprecated(since = "2024-10", forRemoval = true)
    public void addTeamSubmission(RoadViewSubmission submission) {
        addSubmission(submission);
    }

    /**
     * @deprecated Use getRoadViewSubmissions() instead
     * 하위 호환성을 위해 유지
     */
    @Deprecated(since = "2024-10", forRemoval = true)
    public List<RoadViewSubmission> getRoadViewPlayerSubmissions() {
        return getPlayerSubmissions();
    }

    /**
     * @deprecated Use getRoadViewSubmissions() instead
     * 하위 호환성을 위해 유지
     */
    @Deprecated(since = "2024-10", forRemoval = true)
    public List<RoadViewSubmission> getRoadViewTeamSubmissions() {
        return getTeamSubmissions();
    }

    // === create method ===

    public static RoadViewGameRound createRound(
            Integer roundNumber,
            CoordinateNationwide targetCoordinate,
            Integer timeLimit,
            List<Long> playerIds
    ) {
        return RoadViewGameRound.builder()
                .roundNumber(roundNumber)
                .targetCoordinate(targetCoordinate)
                .timeLimit(timeLimit)
                .playerIds(playerIds) //redis
                .isFinished(false)
                .build();
    }
} 