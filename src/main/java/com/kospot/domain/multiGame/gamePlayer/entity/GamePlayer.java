package com.kospot.domain.multiGame.gamePlayer.entity;

import com.kospot.domain.item.entity.Item;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multiGame.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_room_id")
    private GameRoom gameRoom;

    // 협동전의 경우 팀 번호 (1 또는 2)
    private Integer teamNumber;

    private Integer roundRank; // 해당 라운드 순위
    private int totalScore;

    @Enumerated(EnumType.STRING)
    private GamePlayerStatus status;

    //todo chatMessage

    // rank 표시 보류
//    private String rankTier;
//    private String rankLevel;

    private String equippedMarkerImageUrl;

    //business
    public static GamePlayer create(Member member, GameRoom gameRoom) {
        return GamePlayer.builder()
                .member(member)
                .gameRoom(gameRoom)
                .status(GamePlayerStatus.PLAYING)
                .build();
    }

    public void leaveGameRoom(Member member) {
        if (gameRoom != null) {
            this.gameRoom = null;
        }
        this.status = GamePlayerStatus.NONE;
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
