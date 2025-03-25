package com.kospot.domain.multiGame.gameRoom.entity;


import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.entity.GameMode;
import com.kospot.domain.game.entity.GameType;
import com.kospot.domain.member.entity.Member;
import com.kospot.exception.object.domain.GameRoomHandler;
import com.kospot.exception.payload.code.ErrorStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

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

    private boolean privateRoom;

    private int maxPlayers;

    private String password;

    @Enumerated(EnumType.STRING)
    private GameRoomStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private Member host; //방장

    @Builder.Default
    @OneToMany(mappedBy = "gameRoom")
    private Set<Member> waitingPlayers = new HashSet<>();



    //business
    @PreRemove
    private void preRemove() {
        waitingPlayers.forEach(Member::leaveGameRoom);
    }

    public void setHost(Member host) {
        this.host = host;
    }

    public void update(String title, GameMode gameMode, GameType gameType,
                       boolean privateRoom, int maxPlayers, String password) {
        this.title = title;
        this.gameMode = gameMode;
        this.gameType = gameType;
        this.privateRoom = privateRoom;
        this.maxPlayers = maxPlayers;
        this.password = password;
    }

    public void join(Member player) {
        waitingPlayers.add(player);
        player.joinGameRoom(this);
    }

    public void leaveRoom(Member player) {
        waitingPlayers.remove(player);
        player.leaveGameRoom();
    }

    public void kickPlayer(Member host, Member player) {
        validateHost(host);
        leaveRoom(player);
    }

    //player
    public void validateJoinRoom(String inputPassword) {
        validateRoomCapacity();
        if(privateRoom) {
            validatePassword(inputPassword);
        }
        validateRoomStatus();
        validateMemberAlreadyInRoom();
    }

    //todo implement
    private void validateMemberAlreadyInRoom() {

    }

    private void validateRoomCapacity() {
        if (isFull()) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_IS_FULL);
        }
    }

    public void validatePassword(String inputPassword) {
        if (isNotCorrectPassword(inputPassword)) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_IS_NOT_CORRECT_PASSWORD);
        }
    }

    private void validateRoomStatus() {
        if (isNotWaitingRoom()) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_IS_ALREADY_IN_PROGRESS);
        }
    }

    private boolean isNotWaitingRoom() {
        return !status.equals(GameRoomStatus.WAITING);
    }

    private boolean isFull() {
        return getTotalPlayers() >= maxPlayers;
    }

    private int getTotalPlayers() {
        return waitingPlayers.size() + 1;
    }

    public void validateHost(Member gamePlayer) {
        if (isNotHost(gamePlayer)) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_HOST_PRIVILEGES_REQUIRED);
        }
    }

    public boolean isHost(Member gamePlayer) {
        return this.host.getId().equals(gamePlayer.getId());
    }

    private boolean isNotHost(Member gamePlayer) {
        return !this.host.getId().equals(gamePlayer.getId());
    }

    public boolean isRoomEmpty() {
        return waitingPlayers.isEmpty();
    }

    //private room
    public boolean isPublicRoom() {
        return !privateRoom;
    }

    public boolean isNotCorrectPassword(String inputPassword) {
        return !password.equals(inputPassword);
    }

}
