package com.kospot.multi.game.application.service;

import com.kospot.multi.game.domain.entity.MultiRoadViewGame;
import com.kospot.multi.game.domain.vo.PlayerMatchType;
import com.kospot.multi.game.infrastructure.persistence.MultiRoadViewGameRepository;
import com.kospot.multi.room.domain.entity.GameRoom;

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

    public MultiRoadViewGame createGame(GameRoom gameRoom) {
        PlayerMatchType matchType = gameRoom.getPlayerMatchType();
        int totalRounds = gameRoom.getTotalRounds();
        int timeLimit = gameRoom.getTimeLimit();
        boolean poiNameVisible = gameRoom.isPoiNameVisible();
        MultiRoadViewGame game = MultiRoadViewGame.createGame(gameRoom.getId(), matchType, poiNameVisible, totalRounds, timeLimit);
        return multiRoadViewGameRepository.save(game);
    }


}
