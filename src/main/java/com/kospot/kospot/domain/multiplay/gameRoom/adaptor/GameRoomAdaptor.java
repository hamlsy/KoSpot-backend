package com.kospot.kospot.domain.multiplay.gameRoom.adaptor;

import com.kospot.kospot.domain.multiplay.gameRoom.entity.GameRoom;
import com.kospot.kospot.domain.multiplay.gameRoom.repository.GameRoomRepository;
import com.kospot.kospot.exception.object.domain.GameRoomHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import com.kospot.kospot.global.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameRoomAdaptor {

    private final GameRoomRepository repository;

    public GameRoom queryById(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new GameRoomHandler(ErrorStatus.GAME_ROOM_NOT_FOUND)
        );
    }

}
