package com.kospot.domain.gamerank.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gamerank.vo.RankLevel;
import com.kospot.domain.gamerank.vo.RankTier;
import com.kospot.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class GameRank extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_rank_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    private GameMode gameMode;

    private int ratingScore;

    @Enumerated(EnumType.STRING)
    private RankTier rankTier;

    @Enumerated(EnumType.STRING)
    private RankLevel rankLevel;

    //business logic
    public static GameRank create(Member member, GameMode gameMode){
        return GameRank.builder()
                .member(member)
                .ratingScore(0)
                .rankTier(RankTier.BRONZE)
                .rankLevel(RankLevel.FIVE)
                .gameMode(gameMode)
                .build();
    }

    public void changeRatingScore(int score) {
        this.ratingScore += score;
        changeRank();
    }

    public void applyPenaltyForAbandon() {
       this.ratingScore =  Math.max(0, this.ratingScore - 100);
       changeRank();
    }

    private void changeRank(){
        this.rankTier = RankTier.getRankByRating(ratingScore);
        this.rankLevel = RankLevel.getLevelByRating(ratingScore);
    }

}
