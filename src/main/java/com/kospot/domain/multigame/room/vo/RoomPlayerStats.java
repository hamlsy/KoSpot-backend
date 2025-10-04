package com.kospot.domain.multigame.room.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게임방 플레이어 통계 정보 VO
 */
@Getter
@Builder
@RequiredArgsConstructor
public class RoomPlayerStats {

    private final String roomId;

    private final int currentPlayerCount;

    private final long lastUpdated;

    public boolean isEmpty() {
        return currentPlayerCount == 0;
    }

    public boolean isFull(int maxPlayers) {
        return currentPlayerCount >= maxPlayers;
    }

    public boolean canJoin(int maxPlayers) {
        return currentPlayerCount < maxPlayers;
    }

} 