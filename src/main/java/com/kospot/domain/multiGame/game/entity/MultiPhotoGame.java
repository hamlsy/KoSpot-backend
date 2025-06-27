package com.kospot.domain.multiGame.game.entity;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multiGame.game.vo.PlayerMatchType;
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

    // 각 라운드별 보여줄 사진 수(사진 모드에서 사용)
    private Integer photosPerRound;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_room_id")
    private GameRoom gameRoom;
    
    @OneToMany(mappedBy = "multiPhotoGame", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhotoGameRound> photoGameRounds = new ArrayList<>();
    
    // 생성 메서드
    public static MultiPhotoGame createGame(GameRoom gameRoom, PlayerMatchType matchType,
                                           Integer roundCount, Integer photosPerRound) {

        return MultiPhotoGame.builder()
                .matchType(matchType)
                .gameMode(GameMode.PHOTO)  // 사진 모드로 고정
                .totalRounds(roundCount)
                .currentRound(0) // 시작 전에는 0
                .isFinished(false)
                .photosPerRound(photosPerRound)
                .gameRoom(gameRoom)
                .build();
    }
    
    // 게임 라운드 추가
    public void addPhotoGameRound(PhotoGameRound gameRound) {
        this.photoGameRounds.add(gameRound);
        gameRound.setMultiPhotoGame(this);
    }


}