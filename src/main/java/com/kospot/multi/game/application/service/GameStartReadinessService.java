package com.kospot.multi.game.application.service;

import com.kospot.common.exception.object.domain.GameHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import com.kospot.multi.room.domain.vo.MultiplayerScreenState;
import com.kospot.multi.room.infrastructure.redis.service.GameRoomRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameStartReadinessService {

    private static final int MIN_START_PLAYERS = 2;

    private final GameRoomRedisService gameRoomRedisService;

    public void validateReadyToStart(String roomId) {
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(roomId);
        if (players.size() < MIN_START_PLAYERS) {
            throw new GameHandler(ErrorStatus.GAME_ROOM_IS_NOT_ENOUGH_PLAYER);
        }

        List<Long> notReadyMemberIds = players.stream()
                .filter(player -> !MultiplayerScreenState.ROOM.equals(player.getScreenState()))
                .map(GameRoomPlayerInfo::getMemberId)
                .collect(Collectors.toList());

        if (!notReadyMemberIds.isEmpty()) {
            log.info("Reject game start because players are not ready for room - RoomId: {}, NotReadyMembers: {}",
                    roomId,
                    notReadyMemberIds);
            throw new GameHandler(ErrorStatus._BAD_REQUEST);
        }
    }
}
