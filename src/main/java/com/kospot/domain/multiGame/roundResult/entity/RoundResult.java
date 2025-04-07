package com.kospot.domain.multiGame.roundResult.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.multiGame.game.entity.MultiGame;
import com.kospot.domain.multiGame.game.entity.MultiPhotoGame;
import com.kospot.domain.multiGame.game.entity.MultiRoadViewGame;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoundResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_road_view_game_id")
    private MultiRoadViewGame multiRoadViewGame;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_photo_game_id")
    private MultiPhotoGame multiPhotoGame;


    private Integer roundNumber;

    @OneToMany(mappedBy = "roundResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerRoundResult> playerResults = new ArrayList<>();

    private Boolean isProcessed;

    // Business methods
    public void addPlayerResult(PlayerRoundResult result) {
        this.playerResults.add(result);
        result.setRoundResult(this);
    }

    public void markAsProcessed() {
        this.isProcessed = true;
    }

    // 생성 메서드

} 