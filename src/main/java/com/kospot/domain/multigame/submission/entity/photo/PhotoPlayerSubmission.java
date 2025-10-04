package com.kospot.domain.multigame.submission.entity.photo;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.multigame.game.util.ScoreRule;
import com.kospot.domain.multigame.round.entity.PhotoGameRound;
import com.kospot.domain.multigame.gamePlayer.entity.GamePlayer;
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
public class PhotoPlayerSubmission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_game_round_id")
    private PhotoGameRound photoGameRound;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_player_id")
    private GamePlayer gamePlayer;
    
    // 제출 시간 (밀리초)
    private Long submissionTime;
    
    // 정답 맞춘 순서 (1등, 2등, ...)
    private Integer answerOrder;
    
    // 순위에 따른 점수
    private Integer score;
    
    // 팀 게임일 경우 팀 식별자
    private Integer teamNumber;
    
    // Business methods
    public void setPhotoGameRound(PhotoGameRound photoGameRound) {
        this.photoGameRound = photoGameRound;
    }
    
    public void assignAnswerOrder(Integer order) {
        this.answerOrder = order;
        calculateScore();
    }
    
    // 정답 순서에 따른 점수 계산
    private void calculateScore() {
        this.score = ScoreRule.calculateScore(this.answerOrder);
        
        // 플레이어 점수 업데이트
        if (this.gamePlayer != null) {
            this.gamePlayer.addScore(this.score);
        }
    }
    
    public void assignScore(Integer score) {
        this.score = score;
    }
    
    // 생성 메서드
    public static PhotoPlayerSubmission createSubmission(GamePlayer gamePlayer, 
                                                        Long submissionTime, Integer teamNumber) {
        return PhotoPlayerSubmission.builder()
                .gamePlayer(gamePlayer)
                .submissionTime(submissionTime)
                .teamNumber(teamNumber)
                .build();
    }
} 