package com.kospot.domain.multiGame.roundResult.entity;

import com.kospot.domain.multiGame.game.entity.MultiPhotoGame;
import com.kospot.domain.multiGame.gameRound.entity.PhotoGameRound;
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
@Table(name = "photo_round_result")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoRoundResult extends BaseRoundResult {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_photo_game_id")
    private MultiPhotoGame game;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_game_round_id")
    private PhotoGameRound round;

    @OneToMany(mappedBy = "roundResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhotoPlayerRoundResult> playerResults = new ArrayList<>();

    // Business methods
    public void addPlayerResult(PhotoPlayerRoundResult result) {
        this.playerResults.add(result);
        result.setRoundResult(this);
    }

    // 생성 메서드
    public static PhotoRoundResult createRoundResult(MultiPhotoGame game, PhotoGameRound round, Integer roundNumber) {
        return PhotoRoundResult.builder()
                .game(game)
                .round(round)
                .roundNumber(roundNumber)
                .isProcessed(false)
                .build();
    }
} 