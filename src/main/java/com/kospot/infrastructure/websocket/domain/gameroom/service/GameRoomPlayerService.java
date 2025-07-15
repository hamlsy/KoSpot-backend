package com.kospot.infrastructure.websocket.domain.gameroom.service;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multigame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.exception.object.domain.WebSocketHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 게임방 플레이어 관리 서비스
 * 플레이어 입장, 퇴장, 강퇴 등의 비즈니스 로직 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomPlayerService {

    private final MemberAdaptor memberAdaptor;
    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomNotificationService notificationService;
    private final GameRoomSyncService syncService;

    /**
     * 플레이어를 게임방에 추가
     */
    public void addPlayerToRoom(WebSocketMemberPrincipal principal, Long roomId) {
        try {
            // 데이터베이스에서 멤버 정보 조회
            Member member = memberAdaptor.queryById(principal.getMemberId());
            GameRoom gameRoom = gameRoomAdaptor.queryById(roomId);

            // 비즈니스 규칙 검증
            validatePlayerJoin(member, gameRoom, roomId.toString());

            // 입장 전 인원 수 확인
            int previousCount = gameRoomRedisService.getCurrentPlayerCount(roomId.toString());

            // Redis에 플레이어 정보 저장
            GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.from(member);
            gameRoomRedisService.addPlayerToRoom(roomId.toString(), playerInfo);

            // 데이터베이스 업데이트 (멤버의 게임방 정보)
            if (!member.isAlreadyInGameRoom()) {
                member.joinGameRoom(roomId);
            }

            // 입장 후 인원 수 확인
            int currentCount = gameRoomRedisService.getCurrentPlayerCount(roomId.toString());

            // 실시간 알림 전송
            notificationService.notifyPlayerJoinedWithCount(roomId.toString(), playerInfo, previousCount, currentCount);
            
            // DB와 비동기 동기화
            syncService.syncPlayerCountToDatabase(roomId.toString());

            log.info("Player successfully joined room - MemberId: {}, RoomId: {}, Nickname: {}", 
                    member.getId(), roomId, member.getNickname());

        } catch (Exception e) {
            log.error("Failed to add player to room - MemberId: {}, RoomId: {}, Error: {}", 
                    principal.getMemberId(), roomId, e.getMessage());
            throw e;
        }
    }

    /**
     * 플레이어를 게임방에서 제거 (멤버 ID로)
     */
    public void removePlayerFromRoom(Long roomId, Long memberId) {
        try {
            // 퇴장 전 인원 수 확인
            int previousCount = gameRoomRedisService.getCurrentPlayerCount(roomId.toString());
            
            // Redis에서 플레이어 정보 조회 후 삭제
            GameRoomPlayerInfo playerInfo = gameRoomRedisService.removePlayerFromRoom(roomId.toString(), memberId);
            
            if (playerInfo != null) {
                // 데이터베이스 업데이트
                Member member = memberAdaptor.queryById(memberId);
                member.leaveGameRoom();

                // 퇴장 후 인원 수 확인
                int currentCount = gameRoomRedisService.getCurrentPlayerCount(roomId.toString());

                // 실시간 알림 전송
                notificationService.notifyPlayerLeftWithCount(roomId.toString(), playerInfo, previousCount, currentCount);
                
                // DB와 비동기 동기화
                syncService.syncPlayerCountToDatabase(roomId.toString());

                log.info("Player successfully left room - MemberId: {}, RoomId: {}, Nickname: {}", 
                        memberId, roomId, playerInfo.getNickname());
            }

            // 세션 정보 정리
            gameRoomRedisService.cleanupPlayerSession(memberId);

        } catch (Exception e) {
            log.error("Failed to remove player from room - MemberId: {}, RoomId: {}, Error: {}", 
                    memberId, roomId, e.getMessage());
        }
    }

    /**
     * 플레이어를 게임방에서 제거 (세션 ID로)
     */
    public void removePlayerFromRoom(String sessionId) {
        try {
            // 세션에서 룸 ID 조회
            String roomId = gameRoomRedisService.getRoomIdFromSession(sessionId);
            
            if (roomId == null) {
                log.warn("No room found for session: {}", sessionId);
                return;
            }

            // 세션에서 플레이어 ID 조회
            Long memberId = gameRoomRedisService.getMemberIdFromSession(sessionId);
            if (memberId == null) {
                log.warn("No member found for session: {}", sessionId);
                return;
            }

            removePlayerFromRoom(Long.parseLong(roomId), memberId);

        } catch (Exception e) {
            log.error("Failed to remove player from room - SessionId: {}, Error: {}", 
                    sessionId, e.getMessage());
        }
    }

    /**
     * 플레이어 강퇴
     */
    public void kickPlayer(Long roomId, Long hostMemberId, Long targetMemberId) {
        try {
            // 호스트 권한 확인
            GameRoom gameRoom = gameRoomAdaptor.queryById(roomId);
            Member host = memberAdaptor.queryById(hostMemberId);
            gameRoom.validateHost(host);

            // 강퇴 대상 확인
            Member targetMember = memberAdaptor.queryById(targetMemberId);
            if (!targetMember.getGameRoomId().equals(roomId)) {
                throw new WebSocketHandler(ErrorStatus._BAD_REQUEST);
            }

            // 강퇴 전 인원 수 확인
            int previousCount = gameRoomRedisService.getCurrentPlayerCount(roomId.toString());

            // 강퇴 목록에 추가
            gameRoomRedisService.addPlayerToBannedList(roomId.toString(), targetMemberId);

            // 플레이어 제거
            gameRoomRedisService.removePlayerFromRoom(roomId.toString(), targetMemberId);
            targetMember.leaveGameRoom();

            // 강퇴 후 인원 수 확인
            int currentCount = gameRoomRedisService.getCurrentPlayerCount(roomId.toString());

            // 실시간 알림 전송
            notificationService.notifyPlayerKickedWithCount(roomId.toString(), targetMember, previousCount, currentCount);
            
            // DB와 비동기 동기화
            syncService.syncPlayerCountToDatabase(roomId.toString());

            log.info("Player successfully kicked - HostId: {}, TargetId: {}, RoomId: {}", 
                    hostMemberId, targetMemberId, roomId);

        } catch (Exception e) {
            log.error("Failed to kick player - HostId: {}, TargetId: {}, RoomId: {}, Error: {}", 
                    hostMemberId, targetMemberId, roomId, e.getMessage());
            throw e;
        }
    }

    /**
     * 플레이어 입장 검증
     */
    private void validatePlayerJoin(Member member, GameRoom gameRoom, String roomId) {
        // 플레이어가 이미 다른 방에 있는지 확인
        if (member.isAlreadyInGameRoom() && !member.getGameRoomId().equals(gameRoom.getId())) {
            throw new WebSocketHandler(ErrorStatus.GAME_ROOM_MEMBER_ALREADY_IN_ROOM);
        }

        // Redis 기반 실시간 인원 수 체크 (더 정확하고 빠름)
        if (!gameRoomRedisService.canJoinRoom(roomId, gameRoom.getMaxPlayers())) {
            throw new WebSocketHandler(ErrorStatus.GAME_ROOM_IS_FULL);
        }

        // 강퇴된 플레이어인지 확인
        if (gameRoomRedisService.isPlayerBanned(roomId, member.getId())) {
            throw new WebSocketHandler(ErrorStatus.GAME_ROOM_CANNOT_JOIN_NOW);
        }

        // 게임방 상태 확인 (대기 중인지) - 직접 비즈니스 로직 호출
        gameRoom.validateJoinRoom(member, ""); // 패스워드 검증은 별도 처리
    }

    /**
     * 게임방의 현재 플레이어 목록 조회
     */
    public List<GameRoomPlayerInfo> getRoomPlayers(String roomId) {
        return gameRoomRedisService.getRoomPlayers(roomId);
    }

    /**
     * 게임방 현재 인원 수 조회
     */
    public int getCurrentPlayerCount(String roomId) {
        return gameRoomRedisService.getCurrentPlayerCount(roomId);
    }

    /**
     * 게임방 현재 인원 수 조회 (Long roomId용)
     */
    public int getCurrentPlayerCount(Long roomId) {
        return getCurrentPlayerCount(roomId.toString());
    }

    /**
     * 게임방이 비어있는지 확인
     */
    public boolean isRoomEmpty(String roomId) {
        return gameRoomRedisService.isRoomEmpty(roomId);
    }

    /**
     * 게임방 입장 가능 여부 확인
     */
    public boolean canJoinRoom(String roomId, int maxPlayers) {
        return gameRoomRedisService.canJoinRoom(roomId, maxPlayers);
    }

    /**
     * 강퇴된 플레이어인지 확인
     */
    public boolean isPlayerBanned(String roomId, Long memberId) {
        return gameRoomRedisService.isPlayerBanned(roomId, memberId);
    }

    /**
     * 호스트 권한이 있는지 확인
     */
    public boolean isHost(Long roomId, Long memberId) {
        try {
            GameRoom gameRoom = gameRoomAdaptor.queryById(roomId);
            Member member = memberAdaptor.queryById(memberId);
            return gameRoom.isHost(member);
        } catch (Exception e) {
            log.error("Failed to check host privileges - RoomId: {}, MemberId: {}", roomId, memberId, e);
            return false;
        }
    }

    /**
     * 방장 변경 (방장이 나갔을 때)
     */
    @Async("taskExecutor")
    public void changeHost(Long roomId) {
        try {
            GameRoom gameRoom = gameRoomAdaptor.queryById(roomId);
            var players = gameRoomRedisService.getRoomPlayers(roomId.toString());
            
            if (!players.isEmpty()) {
                // 첫 번째 플레이어를 새 방장으로 설정
                GameRoomPlayerInfo newHost = players.get(0);
                Member newHostMember = memberAdaptor.queryById(newHost.getMemberId());
                gameRoom.setHost(newHostMember);
                
                // Redis의 플레이어 정보도 업데이트
                newHost.setHost(true);
                gameRoomRedisService.addPlayerToRoom(roomId.toString(), newHost);
                
                // 방장 변경 알림
                notificationService.notifyRoomSettingsChanged(roomId.toString(), "HOST_CHANGED", newHost.getNickname());
                
                log.info("Host changed - RoomId: {}, NewHostId: {}, NewHostName: {}", 
                        roomId, newHost.getMemberId(), newHost.getNickname());
            }
        } catch (Exception e) {
            log.error("Failed to change host - RoomId: {}", roomId, e);
        }
    }
} 