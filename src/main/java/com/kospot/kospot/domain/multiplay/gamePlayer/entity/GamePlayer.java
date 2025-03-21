package com.kospot.kospot.domain.multiplay.gamePlayer.entity;

import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.multiplay.gameRoom.entity.GameRoom;
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

    private Integer roundRank; // 해당 라운드 순위
    private int totalScore;

    @Enumerated(EnumType.STRING)
    private GamePlayerStatus status;

    //todo marker image, chatMessage
    //todo 연관관계, 아이템 여러개?
    private Item equippedMarker;

    //business
    public void leaveGameRoom(Member member) {
        if (gameRoom != null) {
            gameRoom.playerLeave(member);
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

}
