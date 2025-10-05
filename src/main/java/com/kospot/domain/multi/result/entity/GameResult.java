package com.kospot.domain.multi.result.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.entity.MultiPhotoGame;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
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
public class GameResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_road_view_game_id")
    private MultiRoadViewGame multiRoadViewGame;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_photo_game_id")
    private MultiPhotoGame multiPhotoGame;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_player_id")
    private GamePlayer gamePlayer;

    @Enumerated(EnumType.STRING)
    private GameMode gameMode;

    @Enumerated(EnumType.STRING)
    private PlayerMatchType playerMatchType;
    
    // 최종 점수
    private Integer totalScore;
    
    // 최종 순위
    private Integer finalRank;
    
    // 팀 게임일 경우 팀 식별자 (1 ~ 4)
    private Integer teamNumber;
    
    // 승패 결과 (협동전에서 사용)
    private Boolean isWinner;
    
    // Business methods
    public void setFinalRank(Integer finalRank) {
        this.finalRank = finalRank;
    }
    
    public void setWinner(Boolean isWinner) {
        this.isWinner = isWinner;
    }
    
    // 생성 메서드 - 로드뷰 게임
    public static GameResult createRoadViewResult(MultiRoadViewGame game, GamePlayer gamePlayer, 
                                           Integer totalScore, Integer teamNumber) {
        return GameResult.builder()
                .multiRoadViewGame(game)
                .gamePlayer(gamePlayer)
                .gameMode(GameMode.ROADVIEW)
                .totalScore(totalScore)
                .teamNumber(teamNumber)
                .build();
    }
    
    // 생성 메서드 - 사진 게임
    public static GameResult createPhotoResult(MultiPhotoGame game, GamePlayer gamePlayer, 
                                        Integer totalScore, Integer teamNumber) {
        return GameResult.builder()
                .multiPhotoGame(game)
                .gamePlayer(gamePlayer)
                .gameMode(GameMode.PHOTO)
                .totalScore(totalScore)
                .teamNumber(teamNumber)
                .build();
    }
} 