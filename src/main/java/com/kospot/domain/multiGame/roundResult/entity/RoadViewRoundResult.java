package com.kospot.domain.multiGame.roundResult.entity;

import com.kospot.domain.multiGame.game.entity.MultiRoadViewGame;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@SuperBuilder
@Table(name = "road_view_round_result")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadViewRoundResult extends BaseRoundResult {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_road_view_game_id")
    private MultiRoadViewGame game;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "road_view_game_round_id")
    private RoadViewGameRound round;

    @OneToMany(mappedBy = "roundResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadViewPlayerRoundResult> playerResults = new ArrayList<>();

    // Business methods
    public void addPlayerResult(RoadViewPlayerRoundResult result) {
        this.playerResults.add(result);
        result.setRoundResult(this);
    }

    // 생성 메서드
    public static RoadViewRoundResult createRoundResult(MultiRoadViewGame game, RoadViewGameRound round, Integer roundNumber) {
        return RoadViewRoundResult.builder()
                .game(game)
                .round(round)
                .roundNumber(roundNumber)
                .isProcessed(false)
                .build();
    }
} 