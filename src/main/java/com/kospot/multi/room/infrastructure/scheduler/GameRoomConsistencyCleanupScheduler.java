package com.kospot.multi.room.infrastructure.scheduler;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.multi.room.application.adaptor.GameRoomAdaptor;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import com.kospot.multi.room.infrastructure.persistence.GameRoomRepository;
import com.kospot.multi.room.infrastructure.redis.service.GameRoomRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomConsistencyCleanupScheduler {

    private final GameRoomRedisService gameRoomRedisService;
    private final MemberAdaptor memberAdaptor;
    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomRepository gameRoomRepository;

    @Value("${game-room.consistency.cleanup.enabled:false}")
    private boolean cleanupEnabled;

    @Value("${game-room.consistency.cleanup.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${game-room.consistency.cleanup.fixed-delay-ms:300000}")
    @Transactional
    public void cleanup() {
        if (!cleanupEnabled) {
            return;
        }

        Set<String> activeRoomIds = gameRoomRedisService.getActiveRoomIds();
        if (activeRoomIds.isEmpty()) {
            return;
        }

        int processed = 0;
        int cleaned = 0;

        for (String roomId : activeRoomIds) {
            if (processed >= batchSize) {
                break;
            }

            processed++;
            if (cleanupOneRoom(roomId)) {
                cleaned++;
            }
        }

        int detachedDanglingMembers = cleanupDanglingMembersWithoutExistingRoom(batchSize);

        log.info("Game room consistency cleanup finished - Processed: {}, CleanedRooms: {}, DetachedDanglingMembers: {}, BatchSize: {}",
                processed, cleaned, detachedDanglingMembers, batchSize);
    }

    private boolean cleanupOneRoom(String roomIdString) {
        Long roomId = parseRoomId(roomIdString);
        if (roomId == null) {
            gameRoomRedisService.deleteRoomData(roomIdString);
            return true;
        }

        boolean roomExists = gameRoomAdaptor.existsById(roomId);
        List<Member> dbMembers = memberAdaptor.queryAllByGameRoomId(roomId);
        List<GameRoomPlayerInfo> redisPlayers = gameRoomRedisService.getRoomPlayers(roomIdString);

        if (!roomExists) {
            dbMembers.forEach(Member::leaveGameRoom);
            gameRoomRedisService.deleteRoomData(roomIdString);
            log.warn("Cleaned orphan room state - RoomId: {}, DetachedMembers: {}", roomId, dbMembers.size());
            return true;
        }

        boolean detachedAnyMember = detachDanglingMembers(roomIdString, dbMembers);
        List<Member> remainingMembers = memberAdaptor.queryAllByGameRoomId(roomId);

        if (redisPlayers.isEmpty() && remainingMembers.isEmpty()) {
            gameRoomRepository.deleteById(roomId);
            gameRoomRedisService.deleteRoomData(roomIdString);
            log.warn("Cleaned ghost room - RoomId: {}", roomId);
            return true;
        }

        return detachedAnyMember;
    }

    private boolean detachDanglingMembers(String roomId, List<Member> dbMembers) {
        boolean detachedAnyMember = false;
        for (Member member : dbMembers) {
            if (!gameRoomRedisService.isPlayerInRoom(roomId, member.getId())) {
                member.leaveGameRoom();
                detachedAnyMember = true;
                log.warn("Detached dangling member room state - MemberId: {}, RoomId: {}",
                        member.getId(), roomId);
            }
        }
        return detachedAnyMember;
    }

    private Long parseRoomId(String roomIdString) {
        try {
            return Long.parseLong(roomIdString);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int cleanupDanglingMembersWithoutExistingRoom(int limit) {
        List<Member> membersWithRoom = memberAdaptor.queryAllWithGameRoomId();
        int processed = 0;
        int cleaned = 0;

        for (Member member : membersWithRoom) {
            if (processed >= limit) {
                break;
            }

            processed++;
            Long roomId = member.getGameRoomId();
            if (roomId == null) {
                continue;
            }

            if (!gameRoomAdaptor.existsById(roomId)) {
                member.leaveGameRoom();
                cleaned++;
                log.warn("Detached dangling member with missing room - MemberId: {}, RoomId: {}",
                        member.getId(), roomId);
            }
        }

        return cleaned;
    }
}
