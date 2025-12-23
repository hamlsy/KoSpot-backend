package com.kospot.application.multi.room.http.usecase;

import com.kospot.application.multi.room.vo.LeaveDecision;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.event.GameRoomLeaveEvent;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.exception.object.domain.GameRoomHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.redis.domain.multi.room.adaptor.GameRoomRedisAdaptor;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class LeaveGameRoomUseCase {

    private final MemberAdaptor memberAdaptor;
    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;
    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomRedisAdaptor gameRoomRedisAdaptor;
    private final ApplicationEventPublisher eventPublisher;
    private final RedissonClient redissonClient;

    public void execute(Member member, Long gameRoomId) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        String roomId = gameRoom.getId().toString();
        String lockKey = "lock:game:room:" + roomId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 락 획득 (대기 5초, 자동 해제 10초)
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new GameRoomHandler(ErrorStatus.GAME_ROOM_OPERATION_IN_PROGRESS);
            }
            
            // 락 내부에서 Redis 상태 재검증 및 업데이트
            LeaveDecision decision = makeLeaveDecisionWithLock(gameRoom, member, roomId);
            GameRoomPlayerInfo playerInfo = applyLeaveToRedis(gameRoom, member, decision, roomId);
            
            // 락 해제 후 DB 작업 (락 밖에서 수행)
            lock.unlock();
            
            applyLeaveToDatabase(member, gameRoom, decision);
            gameRoomRedisService.cleanupPlayerSession(member.getId());
            
            // 이벤트 발행
            eventPublisher.publishEvent(new GameRoomLeaveEvent(gameRoom, member, decision, playerInfo));
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock acquisition interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private LeaveDecision makeLeaveDecisionWithLock(
            GameRoom gameRoom,
            Member member,
            String roomId
    ) {
        if (gameRoom.isNotHost(member)) {
            return LeaveDecision.normalLeave(member);
        }
        
        // 락 내부에서 Redis 상태 재검증
        List<GameRoomPlayerInfo> currentPlayers = 
                gameRoomRedisService.getRoomPlayers(roomId);
        
        // 퇴장 플레이어 정보 찾기
        GameRoomPlayerInfo leavingPlayer = currentPlayers.stream()
                .filter(p -> p.getMemberId().equals(member.getId()))
                .findFirst()
                .orElse(null);
        
        if (leavingPlayer == null) {
            // 이미 퇴장한 경우 방 삭제
            return LeaveDecision.deleteRoom(gameRoom, member);
        }
        
        Long leavingJoinedAt = leavingPlayer.getJoinedAt();
        
        // 퇴장 플레이어보다 늦게 들어온 사람 중 가장 작은 joinedAt 찾기
        Optional<GameRoomPlayerInfo> validCandidate = currentPlayers.stream()
                .filter(p -> !p.getMemberId().equals(member.getId()))
                .filter(p -> p.getJoinedAt() != null && p.getJoinedAt() > leavingJoinedAt)
                .min(Comparator.comparing(GameRoomPlayerInfo::getJoinedAt));
        
        if (validCandidate.isEmpty()) {
            return LeaveDecision.deleteRoom(gameRoom, member);
        }
        
        return LeaveDecision.changeHost(
                gameRoom,
                member,
                validCandidate.get()
        );
    }
    
    private GameRoomPlayerInfo applyLeaveToRedis(
            GameRoom gameRoom,
            Member member,
            LeaveDecision decision,
            String roomId
    ) {
        Long playerId = member.getId();
        
        // 1. 플레이어 제거
        GameRoomPlayerInfo playerInfo = gameRoomRedisService.removePlayerFromRoom(roomId, playerId);
        
        // 2. LeaveDecision에 따른 추가 Redis 작업
        switch (decision.getAction()) {
            case DELETE_ROOM:
                gameRoomRedisService.deleteRoomData(roomId);
                log.info("Game room deleted - RoomId: {}", roomId);
                break;
            case CHANGE_HOST:
                // 방장 변경: 다시 한 번 존재 여부 확인
                GameRoomPlayerInfo newHostInfo = decision.getNewHostInfo();
                List<GameRoomPlayerInfo> remainingPlayers = 
                        gameRoomRedisService.getRoomPlayers(roomId);
                
                boolean isNewHostStillInRoom = remainingPlayers.stream()
                        .anyMatch(p -> p.getMemberId().equals(newHostInfo.getMemberId()));
                
                if (isNewHostStillInRoom) {
                    newHostInfo.setHost(true);
                    gameRoomRedisService.savePlayerToRoom(roomId, newHostInfo);
                    log.info("Host changed - RoomId: {}, NewHostId: {}, NewHostName: {}",
                            gameRoom.getId(), newHostInfo.getMemberId(), newHostInfo.getNickname());
                } else {
                    // Fallback: 새 방장이 이미 퇴장한 경우 방 삭제
                    gameRoomRedisService.deleteRoomData(roomId);
                    log.warn("New host already left - RoomId: {}, Deleting room", roomId);
                }
                break;
            case NORMAL_LEAVE:
                // 별도 처리 없음
                break;
        }
        
        return playerInfo;
    }

    private void applyLeaveToDatabase(Member member, GameRoom gameRoom, LeaveDecision decision){
        gameRoomService.leaveGameRoom(member, gameRoom);
        switch(decision.getAction()) {
            case DELETE_ROOM:
                gameRoomService.deleteRoom(gameRoom);
                break;
            case CHANGE_HOST:
                Member newHost = memberAdaptor.queryById(decision.getNewHostInfo().getMemberId());
                gameRoomService.changeHostToMember(gameRoom, newHost);
                break;
            case NORMAL_LEAVE:
                // 별도 처리 없음
                break;
        }
    }

    /**
     * 테스트용: 분산 락 없이 실행하는 메서드
     * 기존 로직을 재현하여 Race Condition 문제를 검증하기 위해 사용
     * 
     * 기존 로직의 문제점:
     * 1. pickNextHostByJoinedAt으로 다음 방장 후보 선택
     * 2. removePlayerFromRoom으로 퇴장 플레이어 제거
     * 3. 재검증 없이 savePlayerToRoom으로 방장 지정
     * 
     * 이로 인해 이미 퇴장한 플레이어가 방장으로 지정될 수 있음
     */
    public void executeWithoutLock(Member member, Long gameRoomId) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        String roomId = gameRoom.getId().toString();
        
        // 기존 로직 재현: 분산 락 없이 실행
        LeaveDecision decision = makeLeaveDecisionWithoutLock(gameRoom, member, roomId);
        GameRoomPlayerInfo playerInfo = applyLeaveToRedisWithoutLock(gameRoom, member, decision, roomId);
        
        applyLeaveToDatabase(member, gameRoom, decision);
        gameRoomRedisService.cleanupPlayerSession(member.getId());
        
        // 이벤트 발행
        eventPublisher.publishEvent(new GameRoomLeaveEvent(gameRoom, member, decision, playerInfo));
    }

    /**
     * 분산 락 없이 다음 방장 결정 (기존 로직 재현)
     */
    private LeaveDecision makeLeaveDecisionWithoutLock(
            GameRoom gameRoom,
            Member member,
            String roomId
    ) {
        if (gameRoom.isNotHost(member)) {
            return LeaveDecision.normalLeave(member);
        }
        
        // 기존 로직: pickNextHostByJoinedAt 사용
        Optional<GameRoomPlayerInfo> nextHostCandidate = 
                gameRoomRedisAdaptor.pickNextHostByJoinedAt(roomId, member.getId());
        
        if (nextHostCandidate.isEmpty()) {
            return LeaveDecision.deleteRoom(gameRoom, member);
        }
        
        return LeaveDecision.changeHost(
                gameRoom,
                member,
                nextHostCandidate.get()
        );
    }

    /**
     * 분산 락 없이 Redis에 적용 (기존 로직 재현 - 재검증 없이)
     */
    private GameRoomPlayerInfo applyLeaveToRedisWithoutLock(
            GameRoom gameRoom,
            Member member,
            LeaveDecision decision,
            String roomId
    ) {
        Long playerId = member.getId();
        
        // 1. 플레이어 제거
        GameRoomPlayerInfo playerInfo = gameRoomRedisService.removePlayerFromRoom(roomId, playerId);
        
        // 2. LeaveDecision에 따른 추가 Redis 작업
        switch (decision.getAction()) {
            case DELETE_ROOM:
                gameRoomRedisService.deleteRoomData(roomId);
                log.info("Game room deleted - RoomId: {}", roomId);
                break;
            case CHANGE_HOST:
                // 기존 로직: 재검증 없이 바로 방장 지정 (문제 발생 지점)
                GameRoomPlayerInfo newHostInfo = decision.getNewHostInfo();
                newHostInfo.setHost(true);
                gameRoomRedisService.savePlayerToRoom(roomId, newHostInfo);
                log.info("Host changed (without lock) - RoomId: {}, NewHostId: {}, NewHostName: {}",
                        gameRoom.getId(), newHostInfo.getMemberId(), newHostInfo.getNickname());
                log.warn("⚠️ 재검증 없이 방장 지정 - 이미 퇴장한 플레이어가 방장으로 지정될 수 있음");
                break;
            case NORMAL_LEAVE:
                // 별도 처리 없음
                break;
        }
        
        return playerInfo;
    }

}
