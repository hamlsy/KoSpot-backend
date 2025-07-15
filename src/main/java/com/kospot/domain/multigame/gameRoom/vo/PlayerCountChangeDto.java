package com.kospot.domain.multigame.gameRoom.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게임방 플레이어 수 변경 이벤트 VO
 */
@Getter
@Builder
@RequiredArgsConstructor
public class PlayerCountChangeDto {
    
    /**
     * 게임방 ID
     */
    private final String roomId;
    
    /**
     * 이전 플레이어 수
     */
    private final int previousCount;
    
    /**
     * 현재 플레이어 수
     */
    private final int currentCount;
    
    /**
     * 변경 타입 (JOIN, LEAVE, KICK)
     */
    private final String changeType;
    
    /**
     * 변경 시간 (Unix timestamp)
     */
    private final long timestamp;
    
    /**
     * 인원 수 변경량 계산
     * @return 변경량 (양수: 증가, 음수: 감소)
     */
    public int getCountDelta() {
        return currentCount - previousCount;
    }
    
    /**
     * 인원이 증가했는지 확인
     * @return 증가 여부
     */
    public boolean isIncreased() {
        return getCountDelta() > 0;
    }
    
    /**
     * 인원이 감소했는지 확인
     * @return 감소 여부
     */
    public boolean isDecreased() {
        return getCountDelta() < 0;
    }
    
    /**
     * 변경 타입 열거형
     */
    public enum ChangeType {
        JOIN("입장"),
        LEAVE("퇴장"), 
        KICK("강퇴"),
        DISCONNECT("연결 해제");
        
        private final String description;
        
        ChangeType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 플레이어 입장 이벤트 생성
     */
    public static PlayerCountChangeDto playerJoined(String roomId, int previousCount, int currentCount) {
        return PlayerCountChangeDto.builder()
                .roomId(roomId)
                .previousCount(previousCount)
                .currentCount(currentCount)
                .changeType(ChangeType.JOIN.name())
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 플레이어 퇴장 이벤트 생성
     */
    public static PlayerCountChangeDto playerLeft(String roomId, int previousCount, int currentCount) {
        return PlayerCountChangeDto.builder()
                .roomId(roomId)
                .previousCount(previousCount)
                .currentCount(currentCount)
                .changeType(ChangeType.LEAVE.name())
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 플레이어 강퇴 이벤트 생성
     */
    public static PlayerCountChangeDto playerKicked(String roomId, int previousCount, int currentCount) {
        return PlayerCountChangeDto.builder()
                .roomId(roomId)
                .previousCount(previousCount)
                .currentCount(currentCount)
                .changeType(ChangeType.KICK.name())
                .timestamp(System.currentTimeMillis())
                .build();
    }
} 