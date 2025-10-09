package com.kospot.domain.multi.round.entity;

import com.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.submission.entity.roadView.RoadViewPlayerSubmission;
import com.kospot.domain.multi.submission.entity.roadView.RoadViewTeamSubmission;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public GameMode getGameMode() {
        return GameMode.ROADVIEW;
    }
    
    // Business methods
    public void setMultiRoadViewGame(MultiRoadViewGame multiRoadViewGame) {
        this.multiRoadViewGame = multiRoadViewGame;
    }

    // create method
    public static RoadViewGameRound createRound(Integer roundNumber, CoordinateNationwide targetCoordinate, Integer timeLimit, List<Long> playerIds) {
        return RoadViewGameRound.builder()
                .roundNumber(roundNumber)
                .targetCoordinate(targetCoordinate)
                .timeLimit(timeLimit)
                .playerIds(playerIds) //redis
                .isFinished(false)
                .build();
    }


} 