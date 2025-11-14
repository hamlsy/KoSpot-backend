package com.kospot.domain.multi.room.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
import com.kospot.infrastructure.exception.object.domain.GameRoomHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@SQLRestriction("deleted = false")
public class GameRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    private GameMode gameMode;

    @Enumerated(EnumType.STRING)
    private PlayerMatchType playerMatchType;

    private int timeLimit; // 시간 제한

    private boolean privateRoom;

    @Min(2)
    @Max(8)
    @Builder.Default
    private int maxPlayers = 2;

    private int teamCount;

    private String password;

    @Enumerated(EnumType.STRING)
    private GameRoomStatus status;

//    @Builder.Default
//    @Column(nullable = false)
//    private Boolean deleted = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private Member host;

    //business
    public void start(Member host) {
        validateGameStart(host);
        this.status = GameRoomStatus.PLAYING;
    }

    public void setHost(Member host) {
        this.host = host;
    }

    public void update(String title, int timeLimit, GameMode gameMode, PlayerMatchType playerMatchType,
                       boolean privateRoom, String password, int teamCount) {
        this.title = title;
        this.timeLimit = timeLimit;
        this.gameMode = gameMode;
        this.playerMatchType = playerMatchType;
        this.privateRoom = privateRoom;
        this.password = password;
        this.teamCount = teamCount;
    }

    public void join(Member player, String inputPassword, Long gameRoomId) {
        validateJoinRoom(player, inputPassword, gameRoomId);
        player.joinGameRoom(this.id);
    }

    public void leaveRoom(Member player) {
        player.leaveGameRoom();
        // currentPlayerCount는 Redis에서 관리하므로 DB 업데이트는 별도 동기화에서 처리
    }

    public void kickPlayer(Member host, Member player) {
        validateHost(host);
        leaveRoom(player);
    }

//    public void deleteRoom() {
//        this.deleted = true;
//    }

    //--- todo websocket

    //validate
    public void validateJoinRoom(Member player, String inputPassword, Long joinGameRoomId) {
        if (privateRoom) {
            validatePassword(inputPassword);
        }
        validateRoomStatus();
        validatePlayerNotInOtherRoom(player, joinGameRoomId);
    }

    private void validatePlayerNotInOtherRoom(Member player, Long joinGameRoomId) {
        if (player.isAlreadyInOtherGameRoom(joinGameRoomId)) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_MEMBER_ALREADY_IN_ROOM);
        }
    }

    public void validateGameStart(Member host) {
//        validatePlayerCount(currentPlayerCount);
        validateHost(host);
        validateRoomStatus();
    }

    public void validatePassword(String inputPassword) {
        if (isNotCorrectPassword(inputPassword)) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_IS_NOT_CORRECT_PASSWORD);
        }
    }

    private void validateRoomStatus() {
        if (isNotWaitingRoom()) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_CANNOT_JOIN_NOW);
        }
    }

    private void validatePlayerCount(int currentPlayerCount) {
        if (currentPlayerCount < 2) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_IS_NOT_ENOUGH_PLAYER);
        }
    }

    public boolean isRoomEmpty(int currentPlayerCount) {
        return currentPlayerCount == 0;
    }

    private boolean isNotWaitingRoom() {
        return !status.equals(GameRoomStatus.WAITING);
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

    public boolean isPublicRoom() {
        return !privateRoom;
    }

    public boolean isNotCorrectPassword(String inputPassword) {
        return !password.equals(inputPassword);
    }
}