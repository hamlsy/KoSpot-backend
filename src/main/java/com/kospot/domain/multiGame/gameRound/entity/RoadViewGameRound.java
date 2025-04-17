package com.kospot.domain.multiGame.gameRound.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.domain.multiGame.game.entity.MultiRoadViewGame;
import com.kospot.domain.multiGame.submission.entity.roadView.RoadViewPlayerSubmission;
import com.kospot.domain.multiGame.submission.entity.roadView.RoadViewTeamSubmission;
import com.kospot.exception.object.domain.GameRoundHandler;
import com.kospot.exception.payload.code.ErrorStatus;
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
    
    private Integer currentRound;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_road_view_game_id")
    private MultiRoadViewGame multiRoadViewGame;
    
    // 라운드에 사용되는 정답 좌표
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinate_id")
    private CoordinateNationwide targetCoordinate;

    @OneToMany(mappedBy = "roadViewGameRound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadViewPlayerSubmission> roadViewPlayerSubmissions = new ArrayList<>();
    
    @OneToMany(mappedBy = "roadViewGameRound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadViewTeamSubmission> roadViewTeamSubmissions = new ArrayList<>();
    
    private Boolean isFinished;
    
    // Business methods
    public void endRound() {
        validateRoundNotFinished();
        this.isFinished = true;
    }

    public void setMultiRoadViewGame(MultiRoadViewGame multiRoadViewGame) {
        this.multiRoadViewGame = multiRoadViewGame;
        multiRoadViewGame.getRoadViewGameRounds().add(this);
    }
    
    public void addPlayerSubmission(RoadViewPlayerSubmission submission) {
        this.roadViewPlayerSubmissions.add(submission);
        submission.setRoadViewGameRound(this);
    }
    
    public void addTeamSubmission(RoadViewTeamSubmission submission) {
        this.roadViewTeamSubmissions.add(submission);
        submission.setRoadViewGameRound(this);
    }
    
    // create method
    public static RoadViewGameRound createRound(Integer roundNumber, CoordinateNationwide targetCoordinate) {
        return RoadViewGameRound.builder()
                .currentRound(roundNumber)
                .targetCoordinate(targetCoordinate)
                .isFinished(false)
                .build();
    }

    // validate
    public void validateRoundNotFinished() {
        if (this.isFinished) {
            throw new GameRoundHandler(ErrorStatus.ROUND_ALREADY_FINISHED);
        }
    }
} 