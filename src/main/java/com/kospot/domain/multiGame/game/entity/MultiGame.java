package com.kospot.domain.multiGame.game.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.entity.GameMode;
import com.kospot.domain.game.entity.GameType;
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

    @Enumerated(EnumType.STRING)
    private GameType gameType;
    
    // 개인전 또는 협동전
    @Enumerated(EnumType.STRING)
    private PlayerMatchType matchType;
    
    // 게임 모드: 로드뷰 또는 사진
    @Enumerated(EnumType.STRING)
    private GameMode gameMode;

    @Min(1)
    @Max(20)
    private Integer roundCount;

    private Integer currentRound;
    
    // 난이도: 라운드별로 난이도가 증가할지 여부
    private Boolean increasingDifficulty;
    
    // 각 라운드별 보여줄 사진 수(사진 모드에서 사용)
    private Integer photosPerRound;

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
    
    public boolean isPhotoMode() {
        return GameMode.PHOTO.equals(this.gameMode);
    }
    
    // 난이도에 따른 현재 라운드의 사진 수 계산 (사진 모드에서 사용)
    public int calculateCurrentRoundPhotos() {
        if (!increasingDifficulty || photosPerRound == null) {
            return photosPerRound != null ? photosPerRound : 4; // 기본값 4
        }
        
        // 난이도 증가 로직: 라운드가 진행됨에 따라 사진 수 감소
        return Math.max(1, photosPerRound - (currentRound / 5));
    }
}
