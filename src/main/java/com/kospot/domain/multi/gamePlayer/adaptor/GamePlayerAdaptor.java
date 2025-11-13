package com.kospot.domain.multi.gamePlayer.adaptor;

import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.repository.GamePlayerRepository;
import com.kospot.domain.multi.gamePlayer.vo.GamePlayerStatus;
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

    public GamePlayer queryByMemberIdAndGameId(Long memberId, Long gameId) {
        return repository.findByMemberIdAndMultiGameId(memberId, gameId).orElseThrow(
                () -> new GamePlayerHandler(ErrorStatus.GAME_PLAYER_NOT_FOUND)
        );
    }

    public GamePlayer queryByMemberIdAndGameIdAndStatus(Long memberId, Long gameId, GamePlayerStatus status) {
        return repository.findByMemberIdAndRoadViewGameIdAndStatus(memberId, gameId, status).orElseThrow(
                () -> new GamePlayerHandler(ErrorStatus.GAME_PLAYER_NOT_FOUND)
        );
    }

    public List<GamePlayer> queryByMultiRoadViewGameId(Long gameId) {
        return repository.findAllByMultiRoadViewGameId(gameId);
    }

    public List<GamePlayer> queryByMultiRoadViewGameIdWithMember(Long gameId) {
        return repository.findAllByMultiRoadViewGameIdWithMember(gameId);
    }

    public List<GamePlayer> queryByGameIdAndStatusFetchMember(Long gameId, GamePlayerStatus status) {
        return repository.findAllByRoadViewGameIdAndStatusFetchMember(gameId, status);
    }

    public List<GamePlayer> queryByGameIdAndTeamNumber(Long gameId, Integer teamNumber) {
        return repository.findAllByMultiRoadViewGameIdAndTeamNumber(gameId, teamNumber);
    }

    public int countPlayersByRoadViewGameId(Long gameId) {
        return repository.countByMultiRoadViewGameId(gameId);
    }

    public int countPlayersByRoadViewGameIdAndStatus(Long gameId, GamePlayerStatus status) {
        return repository.countByRoadViewGameIdAndStatus(gameId, status);
    }

    public int countTeamsByGameId(Long gameId) {
        return repository.countDistinctTeamsByGameId(gameId);
    }
}
