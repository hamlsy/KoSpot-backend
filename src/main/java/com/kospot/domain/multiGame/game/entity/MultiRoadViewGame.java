package com.kospot.domain.multiGame.game.entity;

import com.kospot.domain.game.entity.GameMode;
import com.kospot.domain.game.entity.GameType;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
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
public class MultiRoadViewGame extends MultiGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_room_id")
    private GameRoom gameRoom;
    
    @OneToMany(mappedBy = "multiRoadViewGame", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadViewGameRound> roadViewGameRounds = new ArrayList<>();
    
    // 생성 메서드
    public static MultiRoadViewGame createGame(GameRoom gameRoom, GameType gameType, PlayerMatchType matchType, 
                                             Integer roundCount, Boolean increasingDifficulty) {
        return MultiRoadViewGame.builder()
                .gameType(gameType)
                .matchType(matchType)
                .gameMode(GameMode.ROADVIEW)  // 로드뷰 모드로 고정
                .roundCount(roundCount)
                .currentRound(0) // 시작 전에는 0
                .isFinished(false)
                .increasingDifficulty(increasingDifficulty)
                .gameRoom(gameRoom)
                .build();
    }
    
    // 게임 라운드 추가
    public void addGameRound(RoadViewGameRound roadViewGameRound) {
        this.roadViewGameRounds.add(roadViewGameRound);
        roadViewGameRound.setMultiRoadViewGame(this);
    }
}
