package com.kospot.kospot.domain.game.entity;

import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.exception.object.domain.GameHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class Game extends BaseTimeEntity {

    // 정답 좌표
    @Column(nullable = false)
    private double targetLng;

    @Column(nullable = false)
    private double targetLat;

    // 제출 좌표
    private double submittedLng;
    private double submittedLat;

    // 정답 시간
    private double answerTime;

    private int currentRatingScore;
    private int ratingScoreChange;

    // 점수
    protected double score;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type")
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode")
    private GameMode gameMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_status")
    private GameStatus gameStatus;

    private LocalDateTime endedAt;    // 게임 종료 시간

    public Game(double targetLat, double targetLng, Member member, GameType gameType, GameMode gameMode, GameStatus gameStatus){
        this.member = member;
        this.gameType = gameType;
        this.gameMode = gameMode;
        this.gameStatus = gameStatus;
        this.targetLat = targetLat;
        this.targetLng = targetLng;
    }

    // business
    public void end(Member member, double submittedLat, double submittedLng, double answerTime){
        validateOwnMember(member);
        validateGameStatus();
        this.gameStatus = GameStatus.COMPLETED;
        this.submittedLat = submittedLat;
        this.submittedLng = submittedLng;
        this.answerTime = answerTime;
        this.endedAt = LocalDateTime.now();
    }

    public void endRank(Member member, double submittedLat, double submittedLng, double answerTime,
                        int currentRatingScore, int ratingScoreChange){
        validateOwnMember(member);
        validateGameStatus();
        this.gameStatus = GameStatus.COMPLETED;
        this.submittedLat = submittedLat;
        this.submittedLng = submittedLng;
        this.answerTime = answerTime;
        this.currentRatingScore = currentRatingScore;
        this.ratingScoreChange = ratingScoreChange;
        this.endedAt = LocalDateTime.now();
    }

    public void updateRatingScore(int currentRatingScore, int changeRatingScore) {
        this.currentRatingScore = currentRatingScore;
        this.ratingScoreChange = changeRatingScore;
    }

    //validation
    private void validateGameStatus(){
        if(isCompleted()){
            throw new GameHandler(ErrorStatus.GAME_IS_ALREADY_COMPLETED);
        }
    }

    private boolean isCompleted(){
        return this.gameStatus == GameStatus.COMPLETED;
    }

    private void validateOwnMember(Member member){
        if(isNotSameMember(member)){
            throw new GameHandler(ErrorStatus.GAME_NOT_SAME_MEMBER);
        }
    }

    private boolean isNotSameMember(Member member){
        return this.member.getId() != member.getId();
    }
}
