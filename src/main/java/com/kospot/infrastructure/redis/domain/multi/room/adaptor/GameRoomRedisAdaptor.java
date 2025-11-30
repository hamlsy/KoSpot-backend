package com.kospot.infrastructure.redis.domain.multi.room.adaptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import com.kospot.infrastructure.redis.domain.multi.room.constants.GameRoomRedisKeyConstants;
import com.kospot.infrastructure.redis.domain.multi.room.dao.GameRoomRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Adaptor
@RequiredArgsConstructor
public class GameRoomRedisAdaptor {

    private final GameRoomRedisRepository repository;
    private final ObjectMapper objectMapper;

    public Long getCurrentPlayersCount(String roomId) {
        String roomKey = GameRoomRedisKeyConstants.getRoomKey(roomId);
        return repository.getPlayerCount(roomKey);
    }

    public List<GameRoomPlayerInfo> getRoomPlayers(String roomId) {
        try {
            String roomKey = getRoomKey(roomId);
            Map<Object, Object> players = repository.findAllPlayers(roomKey);

            return players.values().stream()
                    .map(playerJson ->
                            {
                                try {
                                    return objectMapper.readValue((String) playerJson, GameRoomPlayerInfo.class);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                    )
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get room players from Redis - RoomId: {}", roomId, e);
            return List.of();
        }
    }

    private String getRoomKey(String roomId) {
        return GameRoomRedisKeyConstants.getRoomKey(roomId);
    }

    public Optional<GameRoomPlayerInfo> pickNextHostByJoinedAt(String roomId, Long myMemberId) {
        List<GameRoomPlayerInfo> players = getRoomPlayers(roomId);

        // 내 정보 찾기
        GameRoomPlayerInfo me = players.stream()
                .filter(p -> p.getMemberId().equals(myMemberId))
                .findFirst()
                .orElse(null);

        if (me == null) return Optional.empty(); // 내 정보가 없으면 처리 불가

        Long myJoinedAt = me.getJoinedAt();

        // 내 다음(내가 들어온 시간보다 큰 것들 중 가장 작은 joinedAt)
        return players.stream()
                .filter(p -> !p.getMemberId().equals(myMemberId))          // 나 제외
                .filter(p -> p.getJoinedAt() != null && p.getJoinedAt() > myJoinedAt) // 나보다 늦게 들어온 사람
                .min(Comparator.comparing(GameRoomPlayerInfo::getJoinedAt));
    }


}
