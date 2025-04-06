package com.kospot.domain.multiGame.game.entity;

import com.kospot.domain.game.entity.GameMode;
import com.kospot.domain.game.entity.GameType;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.domain.multiGame.gameRound.entity.PhotoGameRound;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MultiPhotoGame extends MultiGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 난이도: 라운드별로 난이도가 증가할지 여부
    private Boolean increasingDifficulty;

    // 각 라운드별 보여줄 사진 수(사진 모드에서 사용)
    private Integer photosPerRound;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_room_id")
    private GameRoom gameRoom;
    
    @OneToMany(mappedBy = "multiPhotoGame", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhotoGameRound> photoGameRounds = new ArrayList<>();
    
    // 생성 메서드
    public static MultiPhotoGame createGame(GameRoom gameRoom, GameType gameType, PlayerMatchType matchType, 
                                           Integer roundCount, Boolean increasingDifficulty, Integer photosPerRound) {

        return MultiPhotoGame.builder()
                .gameType(gameType)
                .matchType(matchType)
                .gameMode(GameMode.PHOTO)  // 사진 모드로 고정
                .roundCount(roundCount)
                .currentRound(0) // 시작 전에는 0
                .isFinished(false)
                .increasingDifficulty(increasingDifficulty)
                .photosPerRound(photosPerRound)
                .gameRoom(gameRoom)
                .build();
    }
    
    // 게임 라운드 추가
    public void addPhotoGameRound(PhotoGameRound gameRound) {
        this.photoGameRounds.add(gameRound);
        gameRound.setMultiPhotoGame(this);
    }
    
    // 현재 라운드 조회
    public PhotoGameRound getCurrentPhotoGameRound() {
        if (this.getCurrentRound() == null || this.getCurrentRound() <= 0 || this.photoGameRounds.isEmpty()) {
            return null;
        }
        
        return this.photoGameRounds.stream()
                .filter(round -> round.getRoundNumber().equals(this.getCurrentRound()))
                .findFirst()
                .orElse(null);
    }

    // 난이도에 따른 현재 라운드의 사진 수 계산 (사진 모드에서 사용)
    public int calculateCurrentRoundPhotos() {
        if (!increasingDifficulty || photosPerRound == null) {
            return photosPerRound != null ? photosPerRound : 4; // 기본값 4
        }

        // 난이도 증가 로직: 라운드가 진행됨에 따라 사진 수 감소
        return Math.max(1, photosPerRound - (super.getCurrentRound() / 5));
    }
}