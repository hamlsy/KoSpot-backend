package com.kospot.mvp.domain.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.gamerank.vo.RankLevel;
import com.kospot.domain.gamerank.vo.RankTier;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "daily_mvp",
        indexes = {
                @Index(name = "idx_daily_mvp_member_id", columnList = "member_id"),
                @Index(name = "idx_daily_mvp_road_view_game_id", columnList = "road_view_game_id"),
                @Index(name = "idx_daily_mvp_reward_date", columnList = "reward_granted, mvp_date")
        }
)
public class DailyMvp extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate mvpDate;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "road_view_game_id", nullable = false)
    private Long roadViewGameId;

    @Column(nullable = false)
    private String poiName;

    @Column(nullable = false)
    private double gameScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RankTier rankTier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RankLevel rankLevel;

    @Column(nullable = false)
    private int ratingScore;

    @Column(nullable = false)
    private int rewardPoint;

    @Column(name = "reward_granted", nullable = false)
    private boolean rewardGranted;

    @Column(name = "reward_granted_at")
    private LocalDateTime rewardGrantedAt;

    public static DailyMvp create(LocalDate mvpDate, RoadViewGame game, GameRank gameRank, int rewardPoint) {
        return DailyMvp.builder()
                .mvpDate(mvpDate)
                .memberId(game.getMember().getId())
                .roadViewGameId(game.getId())
                .poiName(game.getPoiName())
                .gameScore(game.getScore())
                .rankTier(gameRank.getRankTier())
                .rankLevel(gameRank.getRankLevel())
                .ratingScore(gameRank.getRatingScore())
                .rewardPoint(rewardPoint)
                .rewardGranted(false)
                .build();
    }

    public void updateSnapshot(RoadViewGame game, GameRank gameRank) {
        this.memberId = game.getMember().getId();
        this.roadViewGameId = game.getId();
        this.poiName = game.getPoiName();
        this.gameScore = game.getScore();
        this.rankTier = gameRank.getRankTier();
        this.rankLevel = gameRank.getRankLevel();
        this.ratingScore = gameRank.getRatingScore();
    }

    public void grantReward(LocalDateTime now) {
        this.rewardGranted = true;
        this.rewardGrantedAt = now;
    }
}
