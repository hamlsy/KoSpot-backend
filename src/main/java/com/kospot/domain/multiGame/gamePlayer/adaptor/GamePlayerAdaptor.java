package com.kospot.domain.multiGame.gamePlayer.adaptor;

import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gamePlayer.repository.GamePlayerRepository;
import com.kospot.exception.object.domain.GamePlayerHandler;
import com.kospot.exception.payload.code.ErrorStatus;
import com.kospot.global.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GamePlayerAdaptor {

    private final GamePlayerRepository repository;

    public List<GamePlayer> queryByGameRoomId(Long gameRoomId) {
        return repository.findAllByGameRoomId(gameRoomId);
    }

    public GamePlayer queryById(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new GamePlayerHandler(ErrorStatus.GAME_PLAYER_NOT_FOUND)
        );
    }


    public List<GamePlayer> queryByMultiRoadViewGameId(Long gameId) {
        return repository.findAllByMultiRoadViewGameId(gameId);
    }
}
