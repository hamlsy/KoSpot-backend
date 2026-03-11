package com.kospot.multi.game.application.adaptor;

import com.kospot.multi.game.domain.entity.MultiRoadViewGame;
import com.kospot.multi.game.infrastructure.persistence.MultiRoadViewGameRepository;
import com.kospot.multi.game.domain.vo.MultiGameStatus;
import com.kospot.common.exception.object.domain.GameHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.common.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Adaptor
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MultiRoadViewGameAdaptor {

    private final MultiRoadViewGameRepository repository;

    public MultiRoadViewGame queryById(Long gameId) {
        return repository.findById(gameId).orElseThrow(
                () -> new GameHandler(ErrorStatus.GAME_NOT_FOUND)
        );
    }

    public java.util.Optional<MultiRoadViewGame> findInProgressByGameRoomId(Long gameRoomId) {
        return repository.findInProgressByGameRoomId(gameRoomId);
    }

    @Transactional
    public boolean transitionToInProgressIfPending(Long gameId) {
        return repository.transitionToInProgressIfPending(
                gameId,
                MultiGameStatus.PENDING,
                MultiGameStatus.IN_PROGRESS
        ) == 1;
    }

}
