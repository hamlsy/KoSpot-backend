package com.kospot.domain.multiGame.gameRound.adaptor;

import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multiGame.gameRound.repository.RoadViewGameRoundRepository;
import com.kospot.global.exception.object.domain.GameRoundHandler;
import com.kospot.global.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
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

    public RoadViewGameRound queryByIdFetchPlayerSubmissionAndPlayers(Long id) {
        return repository.findByIdFetchPlayerSubmissionAndPlayers(id).orElseThrow(
                () -> new GameRoundHandler(ErrorStatus.GAME_ROUND_NOT_FOUND)
        );
    }

}
