package com.kospot.kospot.domain.game.adaptor;


import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.repository.RoadViewGameRepository;
import com.kospot.kospot.exception.object.domain.GameHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
