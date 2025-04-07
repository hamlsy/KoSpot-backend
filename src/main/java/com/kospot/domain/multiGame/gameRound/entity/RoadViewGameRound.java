package com.kospot.domain.multiGame.gameRound.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.domain.multiGame.game.entity.MultiRoadViewGame;
import com.kospot.domain.multiGame.submittion.entity.RoadViewPlayerSubmission;
import com.kospot.domain.multiGame.submittion.entity.TeamSubmission;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadViewGameRound extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Integer roundNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_road_view_game_id")
    private MultiRoadViewGame multiRoadViewGame;
    
    // 라운드에 사용되는 정답 좌표
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinate_id")
    private CoordinateNationwide targetCoordinate;
    
    @OneToMany(mappedBy = "gameRound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadViewPlayerSubmission> roadViewPlayerSubmissions = new ArrayList<>();
    
    @OneToMany(mappedBy = "gameRound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamSubmission> teamSubmissions = new ArrayList<>();
    
    private Boolean isFinished;
    
    // Business methods
    public void setMultiRoadViewGame(MultiRoadViewGame multiRoadViewGame) {
        this.multiRoadViewGame = multiRoadViewGame;
    }
    
    public void addPlayerSubmission(RoadViewPlayerSubmission submission) {
        this.roadViewPlayerSubmissions.add(submission);
        submission.setRoadViewGameRound(this);
    }
    
    public void addTeamSubmission(TeamSubmission submission) {
        this.teamSubmissions.add(submission);
        submission.setRoadViewGameRound(this);
    }
    
    public void finishRound() {
        this.isFinished = true;
    }
    
    // 생성 메서드
    public static RoadViewGameRound createRound(Integer roundNumber, CoordinateNationwide targetCoordinate) {
        return RoadViewGameRound.builder()
                .roundNumber(roundNumber)
                .targetCoordinate(targetCoordinate)
                .isFinished(false)
                .build();
    }
} 