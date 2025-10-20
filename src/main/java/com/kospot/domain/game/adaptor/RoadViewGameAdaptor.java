package com.kospot.domain.game.adaptor;

import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.repository.RoadViewGameRepository;
import com.kospot.infrastructure.exception.object.domain.GameHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Adaptor
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoadViewGameAdaptor {

    private final RoadViewGameRepository repository;

    public RoadViewGame queryById(Long gameId) {
        return repository.findById(gameId).orElseThrow(
                () -> new GameHandler(ErrorStatus.GAME_NOT_FOUND)
        );
    }

    public RoadViewGame queryByIdFetchCoordinate(Long gameId) {
        return repository.findByIdFetchCoordinate(gameId).orElseThrow(
                () -> new GameHandler(ErrorStatus.GAME_NOT_FOUND)
        );
    }
}
