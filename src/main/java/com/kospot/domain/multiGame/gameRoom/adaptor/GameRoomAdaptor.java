package com.kospot.domain.multiGame.gameRoom.adaptor;

import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.domain.multiGame.gameRoom.repository.GameRoomRepository;
import com.kospot.exception.object.domain.GameRoomHandler;
import com.kospot.exception.payload.code.ErrorStatus;
import com.kospot.global.annotation.adaptor.Adaptor;
import com.kospot.presentation.multiGame.gameRoom.dto.response.FindGameRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public GameRoom queryByIdFetchPlayers(Long id) {
        return repository.findByIdFetchPlayers(id).orElseThrow(
                () -> new GameRoomHandler(ErrorStatus.GAME_ROOM_NOT_FOUND)
        );
    }

    public List<GameRoom> queryAllByKeyword(String keyword, Pageable pageable) {
        return repository.findAllByKeywordPaging(keyword, pageable);
    }

}
