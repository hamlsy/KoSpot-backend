package com.kospot.multi.room.application.usecase;

import com.kospot.multi.game.application.service.CancelMultiGameService;
import com.kospot.multi.room.application.vo.LeaveDecision;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.multi.game.application.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.multi.game.domain.entity.MultiRoadViewGame;
import com.kospot.multi.player.application.adaptor.GamePlayerAdaptor;
import com.kospot.multi.player.domain.entity.GamePlayer;
import com.kospot.multi.room.domain.entity.GameRoom;
import com.kospot.multi.room.domain.event.GameRoomLeaveEvent;
import com.kospot.multi.room.infrastructure.persistence.GameRoomRepository;
import com.kospot.multi.room.application.service.service.GameRoomService;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import com.kospot.multi.room.domain.vo.GameRoomStatus;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.exception.object.domain.GameRoomHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.common.lock.strategy.HostAssignmentLockStrategy;
import com.kospot.common.lock.vo.HostAssignmentResult;
import com.kospot.common.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.common.websocket.domain.multi.lobby.service.LobbyRoomNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 게임방 퇴장 UseCase
 * 
 * 동시성 제어: HostAssignmentLockStrategy를 통해 방장 재지정 로직 원자적 처리
 * 설정: application.yml의 game-room.lock-strategy로 전략 선택 (기본값: lua)
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class LeaveGameRoomUseCase {

    private final MemberAdaptor memberAdaptor;
    private final GameRoomRepository gameRoomRepository;
    private final GameRoomService gameRoomService;
    private final GameRoomRedisService gameRoomRedisService;
    private final ApplicationEventPublisher eventPublisher;

    // Lock Strategy (Lua Script 권장)
    private final HostAssignmentLockStrategy lockStrategy;

    // 게임 진행 중 퇴장 처리
    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final GamePlayerAdaptor gamePlayerAdaptor;
    private final CancelMultiGameService cancelMultiGameService;

    // notify
    private final LobbyRoomNotificationService lobbyRoomNotificationService;

    public void execute(Long memberId, Long gameRoomId) {
        Member member = memberAdaptor.queryById(memberId);
        // 게임 방이 없을 경우 member leaveGameRoom 실행
        GameRoom gameRoom = gameRoomRepository.findById(gameRoomId).orElse(null);
        if (gameRoom == null) {
            member.leaveGameRoom();
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_NOT_FOUND);
        }

        String roomId = gameRoom.getId().toString();

        // Lock Strategy를 통한 원자적 Redis 작업
        HostAssignmentResult result = lockStrategy.executeWithLock(
                roomId,
                member.getId(),
                () -> performLeaveOperation(roomId, member.getId()));

        if (!result.isSuccess()) {
            log.warn("Leave operation failed - RoomId: {}, MemberId: {}, Error: {}",
                    roomId, member.getId(), result.getErrorMessage());
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_OPERATION_IN_PROGRESS);
        }

        // LeaveDecision 생성
        LeaveDecision decision = convertToLeaveDecision(result, gameRoom, member);

        // DB 작업 (락 밖에서 수행)
        applyLeaveToDatabase(member, gameRoom, decision);
        gameRoomRedisService.cleanupPlayerSession(member.getId());

        // 게임 진행 중 퇴장 시 GamePlayer 상태 업데이트
        if (decision.getAction() != LeaveDecision.Action.DELETE_ROOM) {
            handleGamePlayerAbandonIfPlaying(gameRoom, member);
        }

        // 이벤트 발행
        eventPublisher.publishEvent(new GameRoomLeaveEvent(
                gameRoom, member, decision, result.getLeavingPlayerInfo()));

        log.info("Player left room via {} - RoomId: {}, MemberId: {}, Action: {}",
                lockStrategy.getStrategyName(), roomId, member.getId(), decision.getAction());
    }

    /**
     * Redis 레벨에서 퇴장 처리 수행 (Strategy에서 호출)
     */
    private HostAssignmentResult performLeaveOperation(String roomId, Long memberId) {
        GameRoomPlayerInfo playerInfo = gameRoomRedisService.removePlayerFromRoom(roomId, memberId);
        if (playerInfo == null) {
            return HostAssignmentResult.failure("Player not found in room");
        }
        return HostAssignmentResult.normalLeave(memberId, playerInfo);
    }

    /**
     * HostAssignmentResult를 기존 LeaveDecision으로 변환
     */
    private LeaveDecision convertToLeaveDecision(
            HostAssignmentResult result,
            GameRoom gameRoom,
            Member member) {

        return switch (result.getAction()) {
            case DELETE_ROOM -> LeaveDecision.deleteRoom(gameRoom, member);
            case CHANGE_HOST -> LeaveDecision.changeHost(gameRoom, member, result.getNewHostInfo());
            case NORMAL_LEAVE -> LeaveDecision.normalLeave(member);
        };
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
