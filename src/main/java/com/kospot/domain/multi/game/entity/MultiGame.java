package com.kospot.domain.multi.game.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.game.vo.MultiGameStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class MultiGame extends BaseTimeEntity {

    public abstract Long getId();

    // 방 제목
    private String title;

    // 개인전 또는 협동전
    @Enumerated(EnumType.STRING)
    private PlayerMatchType matchType;
    
    // 게임 모드: 로드뷰 또는 사진
    @Enumerated(EnumType.STRING)
    private GameMode gameMode;

    private Integer timeLimit;

    @Min(1)
    @Max(15)
    private Integer totalRounds;

    private Integer currentRound;

    private Boolean isFinished;

    @Enumerated(EnumType.STRING)
    private MultiGameStatus status;

    // Business methods
    public void startGame() {
        this.currentRound = 1;
        this.isFinished = false;
        this.status = MultiGameStatus.IN_PROGRESS;
    }
    
    public void moveToNextRound() {
        if (isLastRound()) {
            finishGame();
            return;
        }
        this.currentRound++;
    }
    
    public void finishGame() {
        this.isFinished = true;
        this.status = MultiGameStatus.FINISHED;
    }
    
    public boolean isLastRound() {
        return this.currentRound.equals(this.totalRounds);
    }

    public boolean isCooperativeMode() {
        return PlayerMatchType.TEAM.equals(this.matchType);
    }
    
    public boolean isPhotoMode() {
        return GameMode.PHOTO.equals(this.gameMode);
    }

    public void markPending() {
        this.status = MultiGameStatus.PENDING;
        this.isFinished = false;
        this.currentRound = 0;
    }

    public void cancelGame() {
        this.isFinished = true;
        this.status = MultiGameStatus.CANCELLED;
    }

    public boolean isPending() {
        return MultiGameStatus.PENDING.equals(this.status);
    }

    public boolean isInProgress() {
        return MultiGameStatus.IN_PROGRESS.equals(this.status);
    }

    public boolean isCancelled() {
        return MultiGameStatus.CANCELLED.equals(this.status);
    }

}
