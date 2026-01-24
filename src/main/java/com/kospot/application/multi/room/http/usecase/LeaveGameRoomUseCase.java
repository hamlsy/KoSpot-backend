package com.kospot.application.multi.room.http.usecase;

import com.kospot.application.multi.game.service.CancelMultiGameService;
import com.kospot.application.multi.room.vo.LeaveDecision;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.event.GameRoomLeaveEvent;
import com.kospot.domain.multi.room.repository.GameRoomRepository;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.exception.object.domain.GameRoomHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.redis.domain.multi.room.adaptor.GameRoomRedisAdaptor;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.infrastructure.websocket.domain.multi.lobby.service.LobbyRoomNotificationService;
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
    private final GameRoomRepository gameRoomRepository;
    private final GameRoomService gameRoomService;
    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomRedisAdaptor gameRoomRedisAdaptor;
    private final ApplicationEventPublisher eventPublisher;
    private final RedissonClient redissonClient;

    // 게임 진행 중 퇴장 처리
    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final GamePlayerAdaptor gamePlayerAdaptor;
    private final CancelMultiGameService cancelMultiGameService;

    // notify
    private final LobbyRoomNotificationService lobbyRoomNotificationService;

    public void execute(Member member, Long gameRoomId) {
        // 게임 방이 없을 경우 member leaveGameRoom 실행
        GameRoom gameRoom = gameRoomRepository.findById(gameRoomId).orElse(null);
        if (gameRoom == null) {
            member.leaveGameRoom();
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_NOT_FOUND);
        }

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

            // 게임 진행 중 퇴장 시 GamePlayer 상태 업데이트 및 활성 플레이어 검증
            // (방 삭제 시에는 applyLeaveToDatabase에서 이미 처리됨)
            if (decision.getAction() != LeaveDecision.Action.DELETE_ROOM) {
                handleGamePlayerAbandonIfPlaying(gameRoom, member);
            }

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
            String roomId) {
//        if (gameRoom.isNotHost(member)) {
//            return LeaveDecision.normalLeave(member);
//        }

        // 락 내부에서 Redis 상태 재검증
        List<GameRoomPlayerInfo> currentPlayers = gameRoomRedisService.getRoomPlayers(roomId);

        // 퇴장 플레이어 정보 찾기
        GameRoomPlayerInfo leavingPlayer = currentPlayers.stream()
                .filter(p -> p.getMemberId().equals(member.getId()))
                .findFirst()
                .orElse(null);

        if (leavingPlayer == null) {
            // 이미 퇴장한 경우 - 중복 호출 방지 (멱등성)
            log.info("Player already left room - MemberId: {}, RoomId: {}", member.getId(), gameRoom.getId());
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_PLAYER_NOT_FOUND);
        }

        if (!leavingPlayer.isHost()) {
            return LeaveDecision.normalLeave(member);
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
                validCandidate.get());
    }

    private GameRoomPlayerInfo applyLeaveToRedis(
            GameRoom gameRoom,
            Member member,
            LeaveDecision decision,
            String roomId) {
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
                List<GameRoomPlayerInfo> remainingPlayers = gameRoomRedisService.getRoomPlayers(roomId);

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

    private void applyLeaveToDatabase(Member member, GameRoom gameRoom, LeaveDecision decision) {
        gameRoomService.leaveGameRoom(member, gameRoom);
        switch (decision.getAction()) {
            case DELETE_ROOM:
                // 방 삭제 전 진행 중인 게임 취소
                if (GameRoomStatus.PLAYING.equals(gameRoom.getStatus())) {
                    cancelMultiGameService.cancelGameOnRoomDeletion(gameRoom.getId());
                }
                gameRoomService.deleteRoom(gameRoom);
                lobbyRoomNotificationService.notifyRoomDeleted(gameRoom.getId());
                break;
            case CHANGE_HOST:
//                Member newHost = memberAdaptor.queryById(decision.getNewHostInfo().getMemberId());
//                gameRoomService.changeHostToMember(gameRoom, newHost);
                gameRoomRepository.updateHost(gameRoom.getId(), decision.getNewHostInfo().getMemberId());
                break;
            case NORMAL_LEAVE:
                // 별도 처리 없음
                break;
        }
    }

    /**
     * 게임 진행 중 퇴장 시 GamePlayer 상태를 ABANDONED로 변경하고,
     * 활성 플레이어가 0명이면 즉시 게임을 취소합니다.
     */
    private void handleGamePlayerAbandonIfPlaying(GameRoom gameRoom, Member member) {
        if (!GameRoomStatus.PLAYING.equals(gameRoom.getStatus())) {
            return;
        }

        Optional<MultiRoadViewGame> inProgressGame = multiRoadViewGameAdaptor
                .findInProgressByGameRoomId(gameRoom.getId());
        if (inProgressGame.isEmpty()) {
            log.debug("No in-progress game found for room - RoomId: {}", gameRoom.getId());
            return;
        }

        MultiRoadViewGame game = inProgressGame.get();
        Optional<GamePlayer> gamePlayerOpt = gamePlayerAdaptor.findByMemberIdAndGameId(member.getId(), game.getId());
        if (gamePlayerOpt.isEmpty()) {
            log.debug("GamePlayer not found for member - MemberId: {}, GameId: {}", member.getId(), game.getId());
            return;
        }

        GamePlayer gamePlayer = gamePlayerOpt.get();
        if (!gamePlayer.isActive()) {
            log.debug("GamePlayer already abandoned - MemberId: {}, GameId: {}", member.getId(), game.getId());
            return;
        }

        gamePlayer.abandon();
        log.info("GamePlayer abandoned - MemberId: {}, GameId: {}", member.getId(), game.getId());

        // 활성 플레이어가 0명이면 즉시 게임 취소
        boolean cancelled = cancelMultiGameService.cancelIfNoActivePlayers(gameRoom.getId(), game.getId());
        if (cancelled) {
            log.info("Game cancelled due to no active players after abandon - RoomId: {}, GameId: {}",
                    gameRoom.getId(), game.getId());
        }
    }

}
