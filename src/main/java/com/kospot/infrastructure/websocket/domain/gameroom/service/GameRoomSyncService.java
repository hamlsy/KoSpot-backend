package com.kospot.infrastructure.websocket.domain.gameroom.service;

import com.kospot.domain.multigame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 게임방 DB 동기화 서비스
 * Redis와 DB 간의 데이터 동기화 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomSyncService {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomRedisService gameRoomRedisService;

    /**
     * 게임방 인원 수를 DB와 동기화 (비동기 처리)
     */
    @Async("taskExecutor")
    public void syncPlayerCountToDatabase(String roomId) {
        try {
            Long gameRoomId = Long.valueOf(roomId);
            int currentCount = gameRoomRedisService.getCurrentPlayerCount(roomId);
            
            // GameRoom 엔티티의 currentPlayerCount 업데이트
            GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
            gameRoom.updateCurrentPlayerCount(currentCount);
            
            log.debug("Successfully synced player count to DB - RoomId: {}, Count: {}", 
                    roomId, currentCount);
            
        } catch (Exception e) {
            log.error("Failed to sync player count to database - RoomId: {}", roomId, e);
        }
    }

    /**
     * 모든 활성 게임방의 인원 수를 DB와 동기화
     */
    @Async("taskExecutor")
    public void syncAllRoomsPlayerCountToDatabase() {
        try {
            // Redis에서 활성 룸 키들을 조회
            Set<String> activeRoomKeys = gameRoomRedisService.getActiveRoomKeys();
            
            if (activeRoomKeys != null && !activeRoomKeys.isEmpty()) {
                log.info("Starting bulk sync for {} active rooms", activeRoomKeys.size());
                
                for (String key : activeRoomKeys) {
                    String roomId = extractRoomIdFromKey(key);
                    if (roomId != null) {
                        syncPlayerCountToDatabase(roomId);
                    }
                }
                
                log.info("Completed bulk sync for all active rooms");
            }
            
        } catch (Exception e) {
            log.error("Failed to sync all rooms player count to database", e);
        }
    }

    /**
     * 특정 게임방의 모든 데이터를 DB와 동기화
     */
    @Async("taskExecutor")
    public void syncRoomDataToDatabase(String roomId) {
        try {
            // 인원 수 동기화
            syncPlayerCountToDatabase(roomId);
            
            // 필요시 추가 데이터 동기화 로직 구현
            // 예: 게임방 설정, 상태 등
            
            log.debug("Successfully synced all room data to DB - RoomId: {}", roomId);
            
        } catch (Exception e) {
            log.error("Failed to sync room data to database - RoomId: {}", roomId, e);
        }
    }

    /**
     * 게임방 상태를 Redis에서 DB로 동기화
     */
    @Async("taskExecutor")
    public void syncRoomStatusToDatabase(Long roomId, String status) {
        try {
            GameRoom gameRoom = gameRoomAdaptor.queryById(roomId);
            // 상태 업데이트 로직 (필요시 GameRoom 엔티티에 메서드 추가)
            
            log.debug("Successfully synced room status to DB - RoomId: {}, Status: {}", roomId, status);
            
        } catch (Exception e) {
            log.error("Failed to sync room status to database - RoomId: {}, Status: {}", roomId, status, e);
        }
    }

    /**
     * Redis 키에서 룸 ID 추출
     */
    private String extractRoomIdFromKey(String key) {
        // game:room:{roomId}:players -> {roomId} 추출
        try {
            String[] parts = key.split(":");
            return parts.length >= 3 ? parts[2] : null;
        } catch (Exception e) {
            log.error("Failed to extract room ID from key: {}", key, e);
            return null;
        }
    }

    /**
     * DB와 Redis 간의 데이터 일관성 검증
     */
    public void validateDataConsistency(String roomId) {
        try {
            Long gameRoomId = Long.valueOf(roomId);
            
            // DB에서 현재 인원 수 조회
            GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
            int dbCount = gameRoom.getCurrentPlayerCount();
            
            // Redis에서 현재 인원 수 조회
            int redisCount = gameRoomRedisService.getCurrentPlayerCount(roomId);
            
            if (dbCount != redisCount) {
                log.warn("Data inconsistency detected - RoomId: {}, DB Count: {}, Redis Count: {}", 
                        roomId, dbCount, redisCount);
                
                // Redis 데이터를 신뢰하여 DB 업데이트
                syncPlayerCountToDatabase(roomId);
            }
            
        } catch (Exception e) {
            log.error("Failed to validate data consistency - RoomId: {}", roomId, e);
        }
    }

    /**
     * 주기적 데이터 일관성 검증
     */
    @Async("taskExecutor")
    public void validateAllRoomsDataConsistency() {
        try {
            Set<String> activeRoomKeys = gameRoomRedisService.getActiveRoomKeys();
            
            if (activeRoomKeys != null) {
                log.info("Starting data consistency validation for {} rooms", activeRoomKeys.size());
                
                for (String key : activeRoomKeys) {
                    String roomId = extractRoomIdFromKey(key);
                    if (roomId != null) {
                        validateDataConsistency(roomId);
                    }
                }
                
                log.info("Completed data consistency validation");
            }
            
        } catch (Exception e) {
            log.error("Failed to validate all rooms data consistency", e);
        }
    }
} 