package com.kospot.domain.multiGame.game.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
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
public class PlayerSubmission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_round_id")
    private GameRound gameRound;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_player_id")
    private GamePlayer gamePlayer;
    
    private Double latitude;
    
    private Double longitude;
    
    // 프론트에서 계산된 정답과의 거리 (미터 단위)
    private Double distance;
    
    // 라운드에서의 순위
    private Integer rank;
    
    // 순위에 따른 점수
    private Integer score;
    
    // 팀 게임일 경우 팀 식별자
    private Integer teamNumber;
    
    // Business methods
    public void setGameRound(GameRound gameRound) {
        this.gameRound = gameRound;
    }
    
    public void assignRank(Integer rank) {
        this.rank = rank;
    }
    
    public void assignScore(Integer score) {
        this.score = score;
    }
    
    // 생성 메서드
    public static PlayerSubmission createSubmission(GamePlayer gamePlayer, Double latitude, Double longitude, Double distance, Integer teamNumber) {
        return PlayerSubmission.builder()
                .gamePlayer(gamePlayer)
                .latitude(latitude)
                .longitude(longitude)
                .distance(distance)
                .teamNumber(teamNumber)
                .build();
    }
} 