package com.kospot.domain.multiGame.roundResult.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerRoundResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_result_id")
    private RoundResult roundResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_player_id")
    private GamePlayer gamePlayer;

    // 제출 시간 (밀리초)
    private Long submissionTime;

    // 정답 여부
    private Boolean isCorrect;

    // 순위 (1등, 2등, ...)
    // 포토모드: 정답 맞춘 순서
    // 로드뷰모드: 정답과의 거리 순서
    private Integer rank;

    // 로드뷰 모드에서 사용: 정답과의 거리
    private Double distance;

    // 계산된 점수
    private Integer score;

    // 팀 게임일 경우 팀 식별자
    private Integer teamNumber;

    // Business methods
    public void setRoundResult(RoundResult roundResult) {
        this.roundResult = roundResult;
    }

    public void calculateScore() {
        if (!isCorrect) {
            this.score = 0;
            return;
        }

        this.score = ScoreRule.calculateScore(this.rank);
        
        // 플레이어 점수 업데이트
        if (this.gamePlayer != null) {
            this.gamePlayer.addScore(this.score);
        }
    }

    // 순위 설정
    public void assignRank(Integer rank) {
        this.rank = rank;
    }

    // 생성 메서드
    public static PlayerRoundResult createResult(GamePlayer gamePlayer, 
                                               Long submissionTime,
                                               Boolean isCorrect,
                                               Integer rank,
                                               Double distance,
                                               Integer teamNumber) {
        return PlayerRoundResult.builder()
                .gamePlayer(gamePlayer)
                .submissionTime(submissionTime)
                .isCorrect(isCorrect)
                .rank(rank)
                .distance(distance)
                .teamNumber(teamNumber)
                .build();
    }
} 