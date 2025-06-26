package com.kospot.domain.multiGame.gamePlayer.entity;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multiGame.game.entity.MultiPhotoGame;
import com.kospot.domain.multiGame.game.entity.MultiRoadViewGame;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_road_view_game_id")
    private MultiRoadViewGame multiRoadViewGame;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_photo_game_id")
    private MultiPhotoGame multiPhotoGame;

    // 협동전의 경우 팀 번호 (1 또는 2) todo color team 으로 수정
    private Integer teamNumber;

    private Integer roundRank; // 해당 라운드 순위
    private int totalScore;

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
                .member(member)
                .equippedMarkerImageUrl(member.getEquippedMarkerImage().getImageUrl())
                .multiRoadViewGame(game)
                .status(GamePlayerStatus.PLAYING)
                .build();
    }

    public static GamePlayer createPhotoGamePlayer(Member member, MultiPhotoGame game) {
        return GamePlayer.builder()
                .nickname(member.getNickname())
                .member(member)
                .equippedMarkerImageUrl(member.getEquippedMarkerImage().getImageUrl())
                .multiPhotoGame(game)
                .status(GamePlayerStatus.PLAYING)
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

    public void addScore(int points) {
        this.totalScore += points;
    }
}
