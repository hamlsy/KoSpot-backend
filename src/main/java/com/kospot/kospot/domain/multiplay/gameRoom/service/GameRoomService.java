package com.kospot.kospot.domain.multiplay.gameRoom.service;

import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.multiplay.gameRoom.entity.GameRoom;
import com.kospot.kospot.domain.multiplay.gameRoom.repository.GameRoomRepository;
import com.kospot.kospot.presentation.multiplay.gameRoom.dto.request.GameRoomRequest;
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
