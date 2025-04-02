package com.kospot.domain.multiGame.submittion.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamSubmission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_round_id")
    private RoadViewGameRound roadViewGameRound;
    
    // 팀 번호 (1 또는 2)
    private Integer teamNumber;
    
    private Double latitude;
    
    private Double longitude;
    
    // 프론트에서 계산된 정답과의 거리 (미터 단위)
    private Double distance;
    
    // 라운드에서의 팀 순위
    private Integer rank;
    
    // 승패 여부 (협동전에서 사용)
    private Boolean isWinner;
    
    // Business methods
    public void setRoadViewGameRound(RoadViewGameRound roadViewGameRound) {
        this.roadViewGameRound = roadViewGameRound;
    }
    
    public void assignRank(Integer rank) {
        this.rank = rank;
    }
    
    public void setWinner(Boolean isWinner) {
        this.isWinner = isWinner;
    }
    
    // 생성 메서드
    public static TeamSubmission createSubmission(Integer teamNumber, Double latitude, Double longitude, Double distance) {
        return TeamSubmission.builder()
                .teamNumber(teamNumber)
                .latitude(latitude)
                .longitude(longitude)
                .distance(distance)
                .build();
    }
} 