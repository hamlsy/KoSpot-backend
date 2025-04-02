package com.kospot.domain.multiGame.game.entity;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.game.entity.GameType;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
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
    private List<GameRound> gameRounds = new ArrayList<>();
    
    // 생성 메서드
    public static MultiRoadViewGame createGame(GameRoom gameRoom, GameType gameType, PlayerMatchType matchType, Integer roundCount) {
        MultiRoadViewGame game = MultiRoadViewGame.builder()
                .gameType(gameType)
                .matchType(matchType)
                .roundCount(roundCount)
                .currentRound(0) // 시작 전에는 0
                .isFinished(false)
                .gameRoom(gameRoom)
                .build();
                
        return game;
    }
    
    // 게임 라운드 추가
    public void addGameRound(GameRound gameRound) {
        this.gameRounds.add(gameRound);
        gameRound.setMultiRoadViewGame(this);
    }
}
