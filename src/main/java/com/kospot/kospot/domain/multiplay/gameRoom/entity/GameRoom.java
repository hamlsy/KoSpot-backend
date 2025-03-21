package com.kospot.kospot.domain.multiplay.gameRoom.entity;


import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.kospot.domain.game.entity.GameMode;
import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.multiplay.gamePlayer.entity.GamePlayer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    private GameMode gameMode;

    @Enumerated(EnumType.STRING)
    private GameType gameType;

    private Integer currentPlayers;

    private int maxPlayers;
    private String password;

    @Enumerated(EnumType.STRING)
    private GameRoomStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private Member host; //방장

    @OneToMany(mappedBy = "gameRoom")
    private List<Member> players = new ArrayList<>();


    //business
    //todo add general Exception
    public void joinPlayer(Member gamePlayer) {
        if(isFull()){
            throw new IllegalStateException();
        }
        players.add(gamePlayer);
    }

    //todo
    public void playerLeave(Member gamePlayer) {
        players.remove(gamePlayer);
        if(isHost(gamePlayer)) {
            //todo 방 폭파 이벤트 발행
        }
    }

    private boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public void validateHost(Member gamePlayer) {
        if(isNotHost(gamePlayer)) {
            throw new IllegalStateException();
        }
    }

    public boolean isHost(Member gamePlayer) {
        return this.host.equals(gamePlayer);
    }

    private boolean isNotHost(Member gamePlayer) {
        return !this.host.equals(gamePlayer);
    }

}
