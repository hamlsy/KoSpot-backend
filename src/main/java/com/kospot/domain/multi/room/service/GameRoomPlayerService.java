package com.kospot.domain.multi.room.service;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.exception.object.domain.WebSocketHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.websocket.domain.multi.room.service.GameRoomNotificationService;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GameRoomPlayerService {

    private final MemberAdaptor memberAdaptor;
    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomNotificationService notificationService;

    public void removePlayerFromRoom(Long roomId, Long memberId) {
        GameRoomPlayerInfo playerInfo = gameRoomRedisService.removePlayerFromRoom(roomId.toString(), memberId);
        
        if (playerInfo != null) {
            updateMemberGameRoomStatus(memberId);
            notificationService.notifyPlayerLeft(roomId.toString(), playerInfo);
            log.info("Player left - MemberId: {}, RoomId: {}, Nickname: {}", 
                    memberId, roomId, playerInfo.getNickname());
        }

        gameRoomRedisService.cleanupPlayerSession(memberId);
    }

    public void removePlayerFromRoom(String sessionId) {
        String roomId = gameRoomRedisService.getRoomIdFromSession(sessionId);
        if (roomId == null) {
            log.warn("No room found for session: {}", sessionId);
            return;
        }

        Long memberId = gameRoomRedisService.getMemberIdFromSession(sessionId);
        if (memberId == null) {
            log.warn("No member found for session: {}", sessionId);
            return;
        }

        removePlayerFromRoom(Long.parseLong(roomId), memberId);
    }

    public void kickPlayer(Long roomId, Long hostMemberId, Long targetMemberId) {
        validateKickPermission(roomId, hostMemberId, targetMemberId);
        
        Member targetMember = memberAdaptor.queryById(targetMemberId);
        gameRoomRedisService.removePlayerFromRoom(roomId.toString(), targetMemberId);
        targetMember.leaveGameRoom();

        GameRoomPlayerInfo targetPlayerInfo = GameRoomPlayerInfo.from(targetMember);
        notificationService.notifyPlayerKicked(roomId.toString(), targetPlayerInfo);

        log.info("Player kicked - HostId: {}, TargetId: {}, RoomId: {}", 
                hostMemberId, targetMemberId, roomId);
    }

    public List<GameRoomPlayerInfo> getRoomPlayers(String roomId) {
        return gameRoomRedisService.getRoomPlayers(roomId);
    }

    public boolean isRoomEmpty(String roomId) {
        return gameRoomRedisService.isRoomEmpty(roomId);
    }

    public boolean canJoinRoom(String roomId, int maxPlayers) {
        return !gameRoomRedisService.cannotJoinRoom(roomId, maxPlayers);
    }

    public boolean isHost(Long roomId, Long memberId) {
        try {
            GameRoom gameRoom = gameRoomAdaptor.queryById(roomId);
            Member member = memberAdaptor.queryById(memberId);
            return gameRoom.isHost(member);
        } catch (Exception e) {
            log.error("Failed to check host - RoomId: {}, MemberId: {}", roomId, memberId, e);
            return false;
        }
    }

    @Async("taskExecutor")
    public void changeHost(Long roomId) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(roomId);
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(roomId.toString());
        
        if (players.isEmpty()) {
            return;
        }

        GameRoomPlayerInfo newHostInfo = players.get(0);
        assignNewHost(gameRoom, newHostInfo, roomId);
        
        log.info("Host changed - RoomId: {}, NewHostId: {}, NewHostName: {}", 
                roomId, newHostInfo.getMemberId(), newHostInfo.getNickname());
    }

    private void updateMemberGameRoomStatus(Long memberId) {
        Member member = memberAdaptor.queryById(memberId);
        member.leaveGameRoom();
    }

    private void validateKickPermission(Long roomId, Long hostMemberId, Long targetMemberId) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(roomId);
        Member host = memberAdaptor.queryById(hostMemberId);
        gameRoom.validateHost(host);

        Member targetMember = memberAdaptor.queryById(targetMemberId);
        if (!targetMember.getGameRoomId().equals(roomId)) {
            throw new WebSocketHandler(ErrorStatus._BAD_REQUEST);
        }
    }

    private void assignNewHost(GameRoom gameRoom, GameRoomPlayerInfo newHostInfo, Long roomId) {
        Member newHostMember = memberAdaptor.queryById(newHostInfo.getMemberId());
        gameRoom.setHost(newHostMember);
        
        newHostInfo.setHost(true);
        gameRoomRedisService.addPlayerToRoom(roomId.toString(), newHostInfo);
    }
} 