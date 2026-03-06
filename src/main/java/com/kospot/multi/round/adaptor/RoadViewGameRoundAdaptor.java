package com.kospot.multi.round.adaptor;

import com.kospot.multi.round.entity.RoadViewGameRound;
import com.kospot.multi.round.repository.RoadViewGameRoundRepository;
import com.kospot.common.exception.object.domain.GameRoundHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.common.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoadViewGameRoundAdaptor {

    private final RoadViewGameRoundRepository repository;

    public RoadViewGameRound queryById(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new GameRoundHandler(ErrorStatus.GAME_ROUND_NOT_FOUND)
        );
    }

    public RoadViewGameRound queryByIdFetchCoordinate(Long id) {
        return repository.findByIdFetchCoordinate(id).orElseThrow(
                () -> new GameRoundHandler(ErrorStatus.GAME_ROUND_NOT_FOUND)
        );
    }

    public RoadViewGameRound queryByIdFetchGame(Long id) {
        return repository.findByIdFetchGame(id).orElseThrow(
                () -> new GameRoundHandler(ErrorStatus.GAME_ROUND_NOT_FOUND)
        );
    }

    public RoadViewGameRound queryByIdFetchGameForUpdate(Long id) {
        return repository.findByIdFetchGameForUpdate(id).orElseThrow(
                () -> new GameRoundHandler(ErrorStatus.GAME_ROUND_NOT_FOUND)
        );
    }

}
