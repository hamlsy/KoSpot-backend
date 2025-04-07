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
@Table(name = "photo_player_round_result")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoPlayerRoundResult extends BasePlayerRoundResult {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_result_id")
    private PhotoRoundResult roundResult;

    // 사진 게임에서 사용: 답변 순서 (1등, 2등, ...)
    private Integer answerOrder;

    // Business methods
    public void setRoundResult(PhotoRoundResult roundResult) {
        this.roundResult = roundResult;
    }

    @Override
    public void calculateScore() {
        if (!getIsCorrect()) {
            this.score = 0;
            return;
        }

        // 답변 순서에 따른 점수 계산
        switch (answerOrder) {
            case 1:
                this.score = ScoreRule.PHOTO_FIRST_ANSWER;
                break;
            case 2:
                this.score = ScoreRule.PHOTO_SECOND_ANSWER;
                break;
            case 3:
                this.score = ScoreRule.PHOTO_THIRD_ANSWER;
                break;
            default:
                this.score = ScoreRule.PHOTO_OTHER_ANSWER;
        }
        
        applyScoreToPlayer();
    }

    // 생성 메서드
    public static PhotoPlayerRoundResult createResult(GamePlayer gamePlayer, 
                                                 Long submissionTime,
                                                 Boolean isCorrect,
                                                 Integer rank,
                                                 Integer answerOrder,
                                                 Integer teamNumber) {
        return PhotoPlayerRoundResult.builder()
                .gamePlayer(gamePlayer)
                .submissionTime(submissionTime)
                .isCorrect(isCorrect)
                .rank(rank)
                .answerOrder(answerOrder)
                .teamNumber(teamNumber)
                .build();
    }
} 