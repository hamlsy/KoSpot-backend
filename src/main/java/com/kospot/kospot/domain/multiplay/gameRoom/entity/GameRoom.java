package com.kospot.kospot.domain.multiplay.gameRoom.entity;


import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.kospot.domain.game.entity.GameMode;
import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.exception.object.domain.GameRoomHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    @OneToMany(mappedBy = "game_room")
    private Set<Member> waitingPlayers = new HashSet<>();


    //business
    public void setHost(Member host) {
        this.host = host;
        join(host);
    }

    public void join(Member player) {
        waitingPlayers.add(player);
    }

    public void leaveRoom(Member player) {
        waitingPlayers.remove(player);
    }

    public void kickPlayer(Member host, Member player) {
        validateHost(host);
        leaveRoom(player);
    }

    //player
    public void validateJoinRoom(String inputPassword) {
        validateRoomCapacity();
        validatePassword(inputPassword);
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
        return waitingPlayers.size() >= maxPlayers;
    }

    public void validateHost(Member gamePlayer) {
        if (isNotHost(gamePlayer)) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_HOST_PRIVILEGES_REQUIRED);
        }
    }

    public boolean isHost(Member gamePlayer) {
        return this.host.equals(gamePlayer);
    }

    private boolean isNotHost(Member gamePlayer) {
        return !this.host.equals(gamePlayer);
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
