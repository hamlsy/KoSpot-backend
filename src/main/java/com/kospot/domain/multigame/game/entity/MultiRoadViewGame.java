package com.kospot.domain.multigame.game.entity;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multigame.game.vo.PlayerMatchType;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRound.entity.RoadViewGameRound;
import jakarta.persistence.*;
import lombok.*;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_room_id")
    private GameRoom gameRoom;

    @Builder.Default
    @OneToMany(mappedBy = "multiRoadViewGame", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadViewGameRound> roadViewGameRounds = new ArrayList<>();
    
    // 생성 메서드
    public static MultiRoadViewGame createGame(GameRoom gameRoom, PlayerMatchType matchType,
                                             Integer roundCount) {
        return MultiRoadViewGame.builder()
                .matchType(matchType)
                .gameMode(GameMode.ROADVIEW)  // 로드뷰 모드로 고정
                .totalRounds(roundCount)
                .currentRound(0) // 시작 전에는 0
                .isFinished(false)
                .gameRoom(gameRoom)
                .build();
    }
    
    // 게임 라운드 추가
    public void addGameRound(RoadViewGameRound roadViewGameRound) {
        this.roadViewGameRounds.add(roadViewGameRound);
        roadViewGameRound.setMultiRoadViewGame(this);
    }
}
