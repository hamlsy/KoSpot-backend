package com.kospot.domain.multigame.gameRoom.vo;

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
    
    /**
     * 게임방 ID
     */
    private final String roomId;
    
    /**
     * 현재 플레이어 수
     */
    private final int currentPlayerCount;
    
    /**
     * 강퇴된 플레이어 수
     */
    private final int bannedPlayerCount;
    
    /**
     * 마지막 업데이트 시간 (Unix timestamp)
     */
    private final long lastUpdated;
    
    /**
     * 게임방이 비어있는지 확인
     * @return 빈 방 여부
     */
    public boolean isEmpty() {
        return currentPlayerCount == 0;
    }
    
    /**
     * 게임방이 가득 찼는지 확인
     * @param maxPlayers 최대 인원 수
     * @return 가득 찬 방 여부
     */
    public boolean isFull(int maxPlayers) {
        return currentPlayerCount >= maxPlayers;
    }
    
    /**
     * 입장 가능 여부 확인
     * @param maxPlayers 최대 인원 수
     * @return 입장 가능 여부
     */
    public boolean canJoin(int maxPlayers) {
        return currentPlayerCount < maxPlayers;
    }
    
    /**
     * 플레이어 밀도 계산 (0.0 ~ 1.0)
     * @param maxPlayers 최대 인원 수
     * @return 플레이어 밀도
     */
    public double getPlayerDensity(int maxPlayers) {
        if (maxPlayers <= 0) return 0.0;
        return (double) currentPlayerCount / maxPlayers;
    }
} 