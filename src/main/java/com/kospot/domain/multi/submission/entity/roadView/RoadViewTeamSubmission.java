package com.kospot.domain.multi.submission.entity.roadView;

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
public class RoadViewTeamSubmission extends BaseRoadViewSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_round_id")
    private RoadViewGameRound roadViewGameRound;
    
    // 팀 번호 (1, 2, 3, 4)
    private Integer teamNumber;
    
    // 정답까지 걸린 시간(밀리초 단위) - 동점 처리용
    private Double timeToAnswer;
    
    // Business methods
    public void setRound(RoadViewGameRound roadViewGameRound) {
        this.roadViewGameRound = roadViewGameRound;
        roadViewGameRound.addTeamSubmission(this);
    }
    
    /**
     * 팀 점수 규칙: 1등 10점, 2등 6점, 3등 2점, 4등 이상 0점
     */
    @Override
    public void assignScore(Integer rank) {
        this.earnedScore = calculateTeamScore(rank);
    }
    
    private Integer calculateTeamScore(Integer rank) {
        return switch (rank) {
            case 1 -> 10;
            case 2 -> 6;
            case 3 -> 2;
            default -> 0;
        };
    }
    
    // 생성 메서드
    public static RoadViewTeamSubmission createSubmission(
            Integer teamNumber, 
            Double lat, 
            Double lng, 
            Double distance,
            Double timeToAnswer) {
        return RoadViewTeamSubmission.builder()
                .teamNumber(teamNumber)
                .lat(lat)
                .lng(lng)
                .distance(distance)
                .timeToAnswer(timeToAnswer)
                .build();
    }
} 