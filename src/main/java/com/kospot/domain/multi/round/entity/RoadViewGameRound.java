package com.kospot.domain.multi.round.entity;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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
    private Coordinate targetCoordinate;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public GameMode getGameMode() {
        return GameMode.ROADVIEW;
    }


    // === Business methods ===

    public void setMultiRoadViewGame(MultiRoadViewGame multiRoadViewGame) {
        this.multiRoadViewGame = multiRoadViewGame;
    }

    // === create method ===

    public static RoadViewGameRound createRound(
            Integer roundNumber,
            Coordinate targetCoordinate,
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

    public void reassignCoordinate(Coordinate newCoordinate) {
        this.targetCoordinate = newCoordinate;
        resetRoundState();
    }

    public void reassignPlayerIds(List<Long> playerIds) {
        replacePlayerIds(playerIds);
    }
} 