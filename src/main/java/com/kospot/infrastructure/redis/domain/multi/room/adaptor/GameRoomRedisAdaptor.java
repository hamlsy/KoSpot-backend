package com.kospot.infrastructure.redis.domain.multi.room.adaptor;

import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import com.kospot.infrastructure.redis.domain.multi.room.constants.GameRoomRedisKeyConstants;
import com.kospot.infrastructure.redis.domain.multi.room.dao.GameRoomRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Adaptor
@RequiredArgsConstructor
public class GameRoomRedisAdaptor {

    private final GameRoomRedisRepository repository;

    public Long getCurrentPlayers(String roomId) {
        String roomKey = GameRoomRedisKeyConstants.getRoomKey(roomId);
        return repository.getPlayerCount(roomKey);
    }





}
