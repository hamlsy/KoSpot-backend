package com.kospot.domain.multiGame.submission.entity.roadView;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.kospot.domain.multiGame.game.util.ScoreRule.calculateScore;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadViewPlayerSubmission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_round_id")
    private RoadViewGameRound roadViewGameRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_player_id")
    private GamePlayer gamePlayer;

    private Double lat;

    private Double lng;

    // 프론트에서 계산된 정답과의 거리 (미터 단위)
    private Double distance;

    // 라운드에서의 순위
    private Integer rank;

    // 순위에 따른 점수
    private Integer score;

    // 정답까지 걸린 시간(밀리초 단위)
    private Double timeToAnswer;


    // Business methods
    public void setRoadViewGameRound(RoadViewGameRound roadViewGameRound) {
        this.roadViewGameRound = roadViewGameRound;
        roadViewGameRound.addPlayerSubmission(this);
    }

    public void assignRankAndScore(Integer rank) {
        assignRank(rank);
        assignScore(calculateScore(rank));
    }

    private void assignRank(Integer rank) {
        this.rank = rank;
    }

    private void assignScore(Integer score) {
        this.score = score;
    }

    public void setGamePlayer(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    // 생성 메서드
    public static RoadViewPlayerSubmission createSubmission(
            GamePlayer gamePlayer, Double latitude, Double longitude, Double distance, Double timeToAnswer) {
        return RoadViewPlayerSubmission.builder()
                .gamePlayer(gamePlayer)
                .lat(latitude)
                .lng(longitude)
                .distance(distance)
                .timeToAnswer(timeToAnswer)
                .build();
    }
} 