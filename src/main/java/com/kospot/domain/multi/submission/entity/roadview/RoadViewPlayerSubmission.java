package com.kospot.domain.multi.submission.entity.roadview;

import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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

    // 정답까지 걸린 시간(밀리초 단위)
    private Double timeToAnswer;

    // Business methods
    public void setGamePlayer(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    public void setRound(com.kospot.domain.multi.round.entity.RoadViewGameRound roadViewGameRound) {
        this.roadViewGameRound = roadViewGameRound;
        roadViewGameRound.addPlayerSubmission(this);
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