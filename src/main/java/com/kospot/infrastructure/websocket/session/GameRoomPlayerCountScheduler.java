package com.kospot.infrastructure.websocket.session;

import com.kospot.infrastructure.websocket.service.GameRoomRedisService;
import com.kospot.infrastructure.websocket.service.GameRoomSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 게임방 플레이어 수 동기화 스케줄러
 * Redis와 DB 간의 인원 수 정보를 주기적으로 동기화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomPlayerCountScheduler {

    private final GameRoomSyncService gameRoomSyncService;
    private final GameRoomRedisService gameRoomRedisService;

    /**
     * 5분마다 모든 활성 게임방의 인원 수를 DB와 동기화
     * Redis 데이터를 기준으로 DB의 currentPlayerCount 필드를 업데이트
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    public void syncPlayerCountsToDatabase() {
        log.info("Starting scheduled player count synchronization");
        
        try {
            gameRoomSyncService.syncAllRoomsPlayerCountToDatabase();
            log.info("Completed scheduled player count synchronization");
            
        } catch (Exception e) {
            log.error("Failed to complete scheduled player count synchronization", e);
        }
    }

    /**
     * 30분마다 비어있는 게임방의 Redis 데이터 정리
     * 메모리 사용량 최적화를 위해 사용되지 않는 데이터 정리
     */
    @Scheduled(fixedRate = 1800000) // 30분 = 1,800,000ms
    public void cleanupEmptyRooms() {
        log.info("Starting cleanup of empty game rooms");
        
        try {
            gameRoomRedisService.cleanupEmptyRooms();
            log.info("Completed cleanup of empty game rooms");
            
        } catch (Exception e) {
            log.error("Failed to cleanup empty game rooms", e);
        }
    }

    /**
     * 매시간 게임방 통계 로깅
     * 시스템 모니터링 및 디버깅을 위한 정보 출력
     */
    @Scheduled(fixedRate = 3600000) // 1시간 = 3,600,000ms
    public void logGameRoomStatistics() {
        log.info("Generating game room statistics");
        
        try {
            gameRoomRedisService.logRoomStatistics();
            
        } catch (Exception e) {
            log.error("Failed to generate game room statistics", e);
        }
    }
} 