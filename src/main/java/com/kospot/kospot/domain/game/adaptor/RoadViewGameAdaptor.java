package com.kospot.kospot.domain.game.adaptor;


import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.repository.RoadViewGameRepository;
import com.kospot.kospot.exception.object.domain.GameHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import com.kospot.kospot.global.annotation.adaptor.Adaptor;
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
}
