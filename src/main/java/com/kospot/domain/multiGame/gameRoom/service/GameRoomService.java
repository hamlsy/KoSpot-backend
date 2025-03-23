package com.kospot.domain.multiGame.gameRoom.service;

import com.kospot.domain.game.entity.GameMode;
import com.kospot.domain.game.entity.GameType;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.domain.multiGame.gameRoom.repository.GameRoomRepository;
import com.kospot.presentation.multiGame.gameRoom.dto.request.GameRoomRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GameRoomService {

    private final GameRoomRepository gameRoomRepository;

    public GameRoom createGameRoom(Member host, GameRoomRequest.Create request) {
        GameRoom gameRoom = request.toEntity();
        gameRoom.setHost(host);

        return gameRoomRepository.save(gameRoom);
    }

    public GameRoom updateGameRoom(Member host, GameRoomRequest.Update request, GameRoom gameRoom) {
        gameRoom.validateHost(host);
        gameRoom.update(request.getTitle(), GameMode.fromKey(request.getGameModeKey()), GameType.fromKey(request.getGameTypeKey()),
                request.isPrivateRoom(), request.getMaxPlayers(), request.getPassword());
        return gameRoom;
    }

    //todo 동시성 해결
    public void joinGameRoom(Member player, GameRoom gameRoom, GameRoomRequest.Join request) {
        //validate join, todo implement validate already in room
        gameRoom.validateJoinRoom(request.getPassword());
        gameRoom.join(player);

        //todo websocket 입장 알림 전송
    }

    public void leaveGameRoom(Member player, GameRoom gameRoom) {
        //플레이어가 나간 경우
        gameRoom.leaveRoom(player);

        //방장이 나간 경우 또는 남은 플레이어가 없는 경우
        if (gameRoom.isHost(player) || gameRoom.isRoomEmpty()) {
            deleteRoom(gameRoom);
        }

    }

    private void deleteRoom(GameRoom gameRoom) {
        gameRoomRepository.delete(gameRoom);
    }

    public void kickPlayer(Member host, Member targetPlayer, GameRoom gameRoom) {
        gameRoom.kickPlayer(host, targetPlayer);
    }


}
