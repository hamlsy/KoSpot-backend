package com.kospot.domain.multiGame.roundResult.entity;

import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.game.util.ScoreRule;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@SuperBuilder
@Table(name = "road_view_player_round_result")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadViewPlayerRoundResult extends BasePlayerRoundResult {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_result_id")
    private RoadViewRoundResult roundResult;

    // 로드뷰 모드에서 사용: 정답과의 거리
    private Double distance;

    // Business methods
    public void setRoundResult(RoadViewRoundResult roundResult) {
        this.roundResult = roundResult;
    }

    @Override
    public void calculateScore() {
        if (!getIsCorrect()) {
            this.score = 0;
            return;
        }

        // 거리에 따른 기본 점수 계산
        int distanceScore;
        if (this.distance <= 50) {
            distanceScore = 100;
        } else if (this.distance <= 200) {
            distanceScore = 80;
        } else {
            distanceScore = Math.max(0, 100 - (int)(this.distance / 10));
        }
        
        // 순위에 따른 추가 점수
        int rankScore = ScoreRule.calculateScore(getRank());
        
        // 최종 점수 계산
        int finalScore = distanceScore + rankScore;
        
        this.score = finalScore;
        applyScoreToPlayer();
    }

    // 생성 메서드
    public static RoadViewPlayerRoundResult createResult(GamePlayer gamePlayer, 
                                                      Long submissionTime,
                                                      Boolean isCorrect,
                                                      Integer rank,
                                                      Double distance,
                                                      Integer teamNumber) {
        return RoadViewPlayerRoundResult.builder()
                .gamePlayer(gamePlayer)
                .submissionTime(submissionTime)
                .isCorrect(isCorrect)
                .rank(rank)
                .distance(distance)
                .teamNumber(teamNumber)
                .build();
    }
} 