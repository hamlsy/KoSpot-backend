package com.kospot.domain.multiGame.game.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.coordinate.entity.Coordinate;
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
public class GameRound extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Integer roundNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_road_view_game_id")
    private MultiRoadViewGame multiRoadViewGame;
    
    // 라운드에 사용되는 좌표 (정답)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinate_id")
    private Coordinate targetCoordinate;
    
    @OneToMany(mappedBy = "gameRound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerSubmission> playerSubmissions = new ArrayList<>();
    
    @OneToMany(mappedBy = "gameRound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamSubmission> teamSubmissions = new ArrayList<>();
    
    private Boolean isFinished;
    
    // Business methods
    public void setMultiRoadViewGame(MultiRoadViewGame multiRoadViewGame) {
        this.multiRoadViewGame = multiRoadViewGame;
    }
    
    public void addPlayerSubmission(PlayerSubmission submission) {
        this.playerSubmissions.add(submission);
        submission.setGameRound(this);
    }
    
    public void addTeamSubmission(TeamSubmission submission) {
        this.teamSubmissions.add(submission);
        submission.setGameRound(this);
    }
    
    public void finishRound() {
        this.isFinished = true;
    }
    
    // 생성 메서드
    public static GameRound createRound(Integer roundNumber, Coordinate targetCoordinate) {
        return GameRound.builder()
                .roundNumber(roundNumber)
                .targetCoordinate(targetCoordinate)
                .isFinished(false)
                .build();
    }
} 