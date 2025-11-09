package com.kospot.domain.multi.game.entity;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.room.entity.GameRoom;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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
    
    // 생성 메서드
    public static MultiRoadViewGame createGame(GameRoom gameRoom, PlayerMatchType matchType,
                                               Integer roundCount, Integer timeLimit) {
        MultiRoadViewGame game = MultiRoadViewGame.builder()
                .matchType(matchType)
                .gameMode(GameMode.ROADVIEW)  // 로드뷰 모드로 고정
                .totalRounds(roundCount)
                .timeLimit(gameRoom.getTimeLimit())
                .currentRound(0) // 시작 전에는 0
                .timeLimit(timeLimit)
                .isFinished(false)
                .gameRoom(gameRoom)
                .build();
        game.markPending();
        return game;
    }

    @Override
    public Long getId() {
        return this.id;
    }


}
