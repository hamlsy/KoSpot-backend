package com.kospot.domain.multi.round.adaptor;

import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.repository.RoadViewGameRoundRepository;
import com.kospot.infrastructure.exception.object.domain.GameRoundHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
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

    /**
     * 제출 데이터와 플레이어 정보 함께 조회 (통합)
     */
    public RoadViewGameRound queryByIdFetchSubmissionsAndPlayers(Long id) {
        return repository.findByIdFetchSubmissionsAndPlayers(id).orElseThrow(
                () -> new GameRoundHandler(ErrorStatus.GAME_ROUND_NOT_FOUND)
        );
    }

    /**
     * 제출 데이터만 조회 (통합)
     */
    public RoadViewGameRound queryByIdFetchSubmissions(Long id) {
        return repository.findByIdFetchSubmissions(id).orElseThrow(
                () -> new GameRoundHandler(ErrorStatus.GAME_ROUND_NOT_FOUND)
        );
    }
}
