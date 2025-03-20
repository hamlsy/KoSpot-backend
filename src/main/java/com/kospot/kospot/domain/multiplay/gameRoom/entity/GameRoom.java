package com.kospot.kospot.domain.multiplay.gameRoom.entity;


import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.kospot.domain.game.entity.GameMode;
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

    private GameMode gameMode;

    private Integer currentPlayers;

    private Integer maxPlayers;

    private String password;

    @Enumerated(EnumType.STRING)
    private GameRoomStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private Member host; //방장

    @OneToMany(mappedBy = "gameRoom")
    private List<GamePlayer> players = new ArrayList<>();

    //todo add general Exception
    public void addPlayer(GamePlayer gamePlayer) {
        if(isFull()){
            throw new IllegalStateException();
        }
        players.add(gamePlayer);
    }

    public void removePlayer(GamePlayer gamePlayer) {
        players.remove(gamePlayer);
    }

    private boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public void validateHost(Member member) {
        if(isNotHost(member)) {
            throw new IllegalStateException();
        }
    }

    public boolean isHost(Member member) {
        return this.host.equals(member);
    }

    private boolean isNotHost(Member member) {
        return !this.host.equals(member);
    }

}
