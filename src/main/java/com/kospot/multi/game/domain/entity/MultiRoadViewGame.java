package com.kospot.multi.game.domain.entity;

import com.kospot.game.domain.vo.GameMode;
import com.kospot.multi.game.domain.vo.PlayerMatchType;
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

    // 생성 메서드
    public static MultiRoadViewGame createGame(Long gameRoomId, PlayerMatchType matchType, boolean poiNameVisible,
                                               Integer roundCount, Integer timeLimit) {
        MultiRoadViewGame game = MultiRoadViewGame.builder()
                .matchType(matchType)
                .gameMode(GameMode.ROADVIEW)  // 로드뷰 모드로 고정
                .totalRounds(roundCount)
                .timeLimit(timeLimit)
                .poiNameVisible(poiNameVisible)
                .currentRound(0) // 시작 전에는 0
                .timeLimit(timeLimit)
                .isFinished(false)
                .gameRoomId(gameRoomId)
                .build();
        game.markPending();
        return game;
    }

    @Override
    public Long getId() {
        return this.id;
    }


}
