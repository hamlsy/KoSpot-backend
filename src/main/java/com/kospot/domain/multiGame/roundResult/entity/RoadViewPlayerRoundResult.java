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