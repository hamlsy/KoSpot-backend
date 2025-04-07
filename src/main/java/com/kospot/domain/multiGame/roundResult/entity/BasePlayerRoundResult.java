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
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BasePlayerRoundResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_player_id")
    private GamePlayer gamePlayer;

    // 제출 시간 (밀리초)
    private Long submissionTime;

    // 정답 여부
    private Boolean isCorrect;

    // 순위 (1등, 2등, ...)
    private Integer rank;

    // 계산된 점수
    protected Integer score;

    // 팀 게임일 경우 팀 식별자
    private Integer teamNumber;

    // 순위 설정
    public void assignRank(Integer rank) {
        this.rank = rank;
    }

    // 점수 계산 추상 메서드 - 각 게임 모드별로 구현
    public abstract void calculateScore();

    // 플레이어 점수 적용
    protected void applyScoreToPlayer() {
        if (this.gamePlayer != null && this.score != null) {
            this.gamePlayer.addScore(this.score);
        }
    }
} 