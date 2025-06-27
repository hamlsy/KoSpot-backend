package com.kospot.domain.multiGame.gameRoom.adaptor;

import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.domain.multiGame.gameRoom.repository.GameRoomRepository;
import com.kospot.infrastructure.exception.object.domain.GameRoomHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
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

    public GameRoom queryByIdFetchHost(Long id) {
        return repository.findByIdFetchHost(id).orElseThrow(
                () -> new GameRoomHandler(ErrorStatus.GAME_ROOM_NOT_FOUND)
        );
    }

    public List<GameRoom> queryAllByKeyword(String keyword, Pageable pageable) {
        return repository.findAllByKeywordPaging(keyword, pageable);
    }

    public List<GameRoom> queryAllPaging(Pageable pageable) {
        return repository.findAllPaging(pageable);
    }

    public List<GameRoom> queryAllWithWaitingFirst(Pageable pageable) {
        return repository.findAllWithWaitingFirst(pageable);
    }

}
