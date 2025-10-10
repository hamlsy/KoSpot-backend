package com.kospot.domain.multi.gamePlayer.adaptor;

import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.repository.GamePlayerRepository;
import com.kospot.infrastructure.exception.object.domain.GamePlayerHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
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

    public GamePlayer queryById(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new GamePlayerHandler(ErrorStatus.GAME_PLAYER_NOT_FOUND)
        );
    }

    public GamePlayer queryByMemberId(Long memberId) {
        return repository.findByMemberId(memberId).orElseThrow(
                () -> new GamePlayerHandler(ErrorStatus.GAME_PLAYER_NOT_FOUND)
        );
    }

    public List<GamePlayer> queryByMultiRoadViewGameId(Long gameId) {
        return repository.findAllByMultiRoadViewGameId(gameId);
    }

    public List<GamePlayer> queryByGameIdAndTeamNumber(Long gameId, Integer teamNumber) {
        return repository.findAllByMultiRoadViewGameIdAndTeamNumber(gameId, teamNumber);
    }
}
