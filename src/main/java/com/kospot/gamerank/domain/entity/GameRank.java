package com.kospot.gamerank.domain.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.game.domain.vo.GameMode;
import com.kospot.gamerank.domain.vo.RankLevel;
import com.kospot.gamerank.domain.vo.RankTier;
import com.kospot.member.domain.entity.Member;
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
@Table(
        name = "game_rank",
        indexes = {
                @Index(name = "idx_game_rank_member_mode", columnList = "member_id, game_mode"),
                @Index(name = "idx_game_rank_mode_tier_rating", columnList = "game_mode, rank_tier, rating_score")
        }
)
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
