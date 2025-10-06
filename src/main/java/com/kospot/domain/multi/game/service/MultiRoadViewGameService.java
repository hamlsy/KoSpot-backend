package com.kospot.domain.multi.game.service;

import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.game.repository.MultiRoadViewGameRepository;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MultiRoadViewGameService {

    private final MultiRoadViewGameRepository multiRoadViewGameRepository;

    public MultiRoadViewGame createGame(GameRoom gameRoom, MultiGameRequest.Start request) {
        PlayerMatchType matchType = PlayerMatchType.fromKey(request.getPlayerMatchTypeKey());
        int totalRounds = request.getTotalRounds();
        MultiRoadViewGame game = MultiRoadViewGame.createGame(gameRoom, matchType, totalRounds);
        return multiRoadViewGameRepository.save(game);
    }

    public void startGame() {

    }

}
