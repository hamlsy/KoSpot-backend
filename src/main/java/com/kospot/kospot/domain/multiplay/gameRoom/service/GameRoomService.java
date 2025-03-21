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


    //todo create room
    public GameRoom createGameRoom(Member host, GameRoomRequest.Create request) {
        GameRoom gameRoom = request.toEntity();
        gameRoom.setHost(host);

        return gameRoomRepository.save(gameRoom);
    }

    //todo join room


    //todo kick
}
