package com.kospot.domain.multi.gamePlayer.entity;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.game.entity.MultiPhotoGame;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.gamePlayer.vo.GamePlayerStatus;
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
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nickname;

    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_road_view_game_id")
    private MultiRoadViewGame multiRoadViewGame;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_photo_game_id")
    private MultiPhotoGame multiPhotoGame;

    // 협동전의 경우 팀 번호 (1 또는 2) todo color team 으로 수정
    private Integer teamNumber;

    private Integer roundRank; // 해당 라운드 순위
    private double totalScore;

    @Enumerated(EnumType.STRING)
    private GamePlayerStatus status;

    // rank 표시 보류
//    private String rankTier;
//    private String rankLevel;

    private String equippedMarkerImageUrl;

    //business
    public static GamePlayer createRoadViewGamePlayer(Member member, MultiRoadViewGame game) {
        return GamePlayer.builder()
                .nickname(member.getNickname())
                .memberId(member.getId())
                .equippedMarkerImageUrl(member.getEquippedMarkerImage().getImageUrl())
                .multiRoadViewGame(game)
                .status(GamePlayerStatus.PLAYING)
                .roundRank(1) 
                .totalScore(0.0)
                .build();
    }

    public static GamePlayer createPhotoGamePlayer(Member member, MultiPhotoGame game) {
        return GamePlayer.builder()
                .nickname(member.getNickname())
                .memberId(member.getId())
                .equippedMarkerImageUrl(member.getEquippedMarkerImage().getImageUrl())
                .multiPhotoGame(game)
                .status(GamePlayerStatus.PLAYING)
                .roundRank(0) // 초기 순위 0으로 설정
                .totalScore(0.0) // 초기 점수 0으로 설정
                .build();
    }


    public void startGame() {
        this.status = GamePlayerStatus.PLAYING;
    }

    public void finishGame() {
        this.status = GamePlayerStatus.FINISHED;
    }

    public void assignTeam(Integer teamNumber) {
        this.teamNumber = teamNumber;
    }

    public void updateRoundRank(Integer rank) {
        this.roundRank = rank;
    }

    public void addScore(double points) {
        this.totalScore += points;
    }
}
