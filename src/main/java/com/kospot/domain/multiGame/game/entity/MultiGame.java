package com.kospot.domain.multiGame.game.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.entity.GameType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
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

    @Enumerated(EnumType.STRING)
    private GameType gameType;
    
    // 개인전 또는 협동전
    @Enumerated(EnumType.STRING)
    private PlayerMatchType matchType;

    private Integer roundCount;

    private Integer currentRound;

    private Boolean isFinished;
    
    // Business methods
    public void startGame() {
        this.currentRound = 1;
        this.isFinished = false;
    }
    
    public void moveToNextRound() {
        if (this.currentRound < this.roundCount) {
            this.currentRound++;
        } else {
            finishGame();
        }
    }
    
    public void finishGame() {
        this.isFinished = true;
    }
    
    public boolean isLastRound() {
        return this.currentRound.equals(this.roundCount);
    }
    
    public boolean isCooperativeMode() {
        return PlayerMatchType.COOPERATIVE.equals(this.matchType);
    }
}
