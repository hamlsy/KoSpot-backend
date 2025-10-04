package com.kospot.domain.multigame.round.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multigame.game.entity.MultiRoadViewGame;
import com.kospot.domain.multigame.submission.entity.roadView.RoadViewPlayerSubmission;
import com.kospot.domain.multigame.submission.entity.roadView.RoadViewTeamSubmission;
import com.kospot.infrastructure.exception.object.domain.GameRoundHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
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

    @Builder.Default
    @OneToMany(mappedBy = "roadViewGameRound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadViewPlayerSubmission> roadViewPlayerSubmissions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "roadViewGameRound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadViewTeamSubmission> roadViewTeamSubmissions = new ArrayList<>();
    
    private Boolean isFinished;

    @Override
    public GameMode getGameMode() {
        return GameMode.ROADVIEW;
    }

    @Override
    public Duration getDuration() {
        return Duration.
    }
    
    // Business methods
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
                .roundNumber(roundNumber)
                .targetCoordinate(targetCoordinate)
                .isFinished(false)
                .build();
    }


} 