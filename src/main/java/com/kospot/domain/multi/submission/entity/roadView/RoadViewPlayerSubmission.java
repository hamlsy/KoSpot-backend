package com.kospot.domain.multi.submission.entity.roadView;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.kospot.domain.multi.game.util.ScoreRule.calculateScore;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadViewPlayerSubmission extends BaseRoadViewSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_player_id")
    private GamePlayer gamePlayer;

    // 순위에 따른 점수
    private Integer earnedScore;

    // 정답까지 걸린 시간(밀리초 단위)
    private Double timeToAnswer;

    // Business methods
    public void assignRankAndScore(Integer roundRank) {
        assignScore(calculateScore(roundRank));
    }

    private void assignScore(Integer score) {
        this.earnedScore = score;
    }

    public void setGamePlayer(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    // 생성 메서드
    public static RoadViewPlayerSubmission createSubmission(
            GamePlayer gamePlayer, Double lat, Double lng, Double distance, Double timeToAnswer) {
        return RoadViewPlayerSubmission.builder()
                .gamePlayer(gamePlayer)
                .lat(lat)
                .lng(lng)
                .distance(distance)
                .timeToAnswer(timeToAnswer)
                .build();
    }
} 