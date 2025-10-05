package com.kospot.domain.multi.submission.entity.roadView;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
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
public class RoadViewTeamSubmission extends BaseTimeEntity {

    //로드뷰 모드 한정
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_round_id")
    private RoadViewGameRound roadViewGameRound;
    
    // 팀 번호 (1 또는 2)
    private Integer teamNumber;
    
    private Double lat;
    
    private Double lng;
    
    // 프론트에서 계산된 정답과의 거리 (미터 단위)
    private Double distance;
    
    // 승패 여부 (협동전에서 사용)
    private Boolean isWinner;
    
    // Business methods
    public void setRoadViewGameRound(RoadViewGameRound roadViewGameRound) {
        this.roadViewGameRound = roadViewGameRound;
    }
    
    public void setWinner(Boolean isWinner) {
        this.isWinner = isWinner;
    }
    
    // 생성 메서드
    public static RoadViewTeamSubmission createSubmission(Integer teamNumber, Double lat, Double lng, Double distance) {
        return RoadViewTeamSubmission.builder()
                .teamNumber(teamNumber)
                .lat(lat)
                .lng(lng)
                .distance(distance)
                .build();
    }
} 