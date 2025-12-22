package com.kospot.application.multi.room.http.usecase;

import com.kospot.application.multi.room.vo.LeaveDecision;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.event.GameRoomLeaveEvent;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.exception.object.domain.GameRoomHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * LeaveGameRoomUseCase 단위 테스트
 * RedissonClient와 RLock을 모킹하여 분산 락 로직을 검증
 */
@ExtendWith(MockitoExtension.class)
class LeaveGameRoomUseCaseTest {

    @Mock
    private MemberAdaptor memberAdaptor;

    @Mock
    private GameRoomAdaptor gameRoomAdaptor;

    @Mock
    private GameRoomService gameRoomService;

    @Mock
    private GameRoomRedisService gameRoomRedisService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Captor
    private ArgumentCaptor<GameRoomLeaveEvent> eventCaptor;

    @InjectMocks
    private LeaveGameRoomUseCase leaveGameRoomUseCase;

    private Member hostMember;
    private Member playerMember;
    private GameRoom gameRoom;
    private String roomId;

    @BeforeEach
    void setUp() {
        hostMember = Member.builder()
                .id(1L)
                .nickname("호스트")
                .build();

        playerMember = Member.builder()
                .id(2L)
                .nickname("플레이어")
                .build();

        gameRoom = GameRoom.builder()
                .id(100L)
                .host(hostMember)
                .build();

        roomId = gameRoom.getId().toString();

        when(redissonClient.getLock(anyString())).thenReturn(lock);
    }

    @Test
    @DisplayName("일반 플레이어 퇴장 - 정상 동작")
    void shouldLeaveRoomNormally() throws InterruptedException {
        // given
        when(gameRoomAdaptor.queryById(anyLong())).thenReturn(gameRoom);
        when(gameRoom.isNotHost(playerMember)).thenReturn(true);
        when(lock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);

        GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.builder()
                .memberId(playerMember.getId())
                .nickname(playerMember.getNickname())
                .build();
        when(gameRoomRedisService.removePlayerFromRoom(roomId, playerMember.getId()))
                .thenReturn(playerInfo);

        // when
        leaveGameRoomUseCase.execute(playerMember, gameRoom.getId());

        // then
        verify(lock).tryLock(5, 10, TimeUnit.SECONDS);
        verify(gameRoomRedisService).removePlayerFromRoom(roomId, playerMember.getId());
        verify(gameRoomRedisService).cleanupPlayerSession(playerMember.getId());
        verify(gameRoomService).leaveGameRoom(playerMember, gameRoom);
        verify(eventPublisher).publishEvent(any(GameRoomLeaveEvent.class));
        verify(lock).unlock();
    }

    @Test
    @DisplayName("방장 퇴장 시 다음 방장 지정")
    void shouldChangeHostWhenHostLeaves() throws InterruptedException {
        // given
        when(gameRoomAdaptor.queryById(anyLong())).thenReturn(gameRoom);
        when(gameRoom.isNotHost(hostMember)).thenReturn(false);
        when(lock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);

        // 다음 방장 후보
        GameRoomPlayerInfo nextHostCandidate = GameRoomPlayerInfo.builder()
                .memberId(playerMember.getId())
                .nickname(playerMember.getNickname())
                .joinedAt(System.currentTimeMillis() + 1000) // 호스트보다 늦게 들어옴
                .build();

        GameRoomPlayerInfo hostInfo = GameRoomPlayerInfo.builder()
                .memberId(hostMember.getId())
                .nickname(hostMember.getNickname())
                .isHost(true)
                .joinedAt(System.currentTimeMillis())
                .build();

        List<GameRoomPlayerInfo> currentPlayers = Arrays.asList(hostInfo, nextHostCandidate);
        when(gameRoomRedisService.getRoomPlayers(roomId)).thenReturn(currentPlayers);

        when(gameRoomRedisService.removePlayerFromRoom(roomId, hostMember.getId()))
                .thenReturn(hostInfo);

        List<GameRoomPlayerInfo> remainingPlayers = Arrays.asList(nextHostCandidate);
        when(gameRoomRedisService.getRoomPlayers(roomId))
                .thenReturn(currentPlayers) // 첫 번째 호출 (재검증)
                .thenReturn(remainingPlayers); // 두 번째 호출 (방장 변경 확인)

        when(memberAdaptor.queryById(playerMember.getId())).thenReturn(playerMember);

        // when
        leaveGameRoomUseCase.execute(hostMember, gameRoom.getId());

        // then
        verify(lock).tryLock(5, 10, TimeUnit.SECONDS);
        verify(gameRoomRedisService, times(2)).getRoomPlayers(roomId); // 재검증 + 방장 변경 확인
        verify(gameRoomRedisService).removePlayerFromRoom(roomId, hostMember.getId());
        verify(gameRoomRedisService).savePlayerToRoom(eq(roomId), argThat(info -> 
                info.getMemberId().equals(playerMember.getId()) && info.isHost()));
        verify(gameRoomService).leaveGameRoom(hostMember, gameRoom);
        verify(gameRoomService).changeHostToMember(gameRoom, playerMember);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        GameRoomLeaveEvent event = eventCaptor.getValue();
        assertThat(event.getDecision().getAction()).isEqualTo(LeaveDecision.Action.CHANGE_HOST);
        verify(lock).unlock();
    }

    @Test
    @DisplayName("방장 퇴장 시 다음 방장 없으면 방 삭제")
    void shouldDeleteRoomWhenNoNextHost() throws InterruptedException {
        // given
        when(gameRoomAdaptor.queryById(anyLong())).thenReturn(gameRoom);
        when(gameRoom.isNotHost(hostMember)).thenReturn(false);
        when(lock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);

        GameRoomPlayerInfo hostInfo = GameRoomPlayerInfo.builder()
                .memberId(hostMember.getId())
                .nickname(hostMember.getNickname())
                .isHost(true)
                .joinedAt(System.currentTimeMillis())
                .build();

        List<GameRoomPlayerInfo> currentPlayers = Arrays.asList(hostInfo);
        when(gameRoomRedisService.getRoomPlayers(roomId)).thenReturn(currentPlayers);
        when(gameRoomRedisService.removePlayerFromRoom(roomId, hostMember.getId()))
                .thenReturn(hostInfo);

        // when
        leaveGameRoomUseCase.execute(hostMember, gameRoom.getId());

        // then
        verify(lock).tryLock(5, 10, TimeUnit.SECONDS);
        verify(gameRoomRedisService).getRoomPlayers(roomId);
        verify(gameRoomRedisService).removePlayerFromRoom(roomId, hostMember.getId());
        verify(gameRoomRedisService).deleteRoomData(roomId);
        verify(gameRoomService).leaveGameRoom(hostMember, gameRoom);
        verify(gameRoomService).deleteRoom(gameRoom);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        GameRoomLeaveEvent event = eventCaptor.getValue();
        assertThat(event.getDecision().getAction()).isEqualTo(LeaveDecision.Action.DELETE_ROOM);
        verify(lock).unlock();
    }

    @Test
    @DisplayName("락 획득 실패 시 예외 발생")
    void shouldThrowExceptionWhenLockAcquisitionFails() throws InterruptedException {
        // given
        when(gameRoomAdaptor.queryById(anyLong())).thenReturn(gameRoom);
        when(lock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> leaveGameRoomUseCase.execute(playerMember, gameRoom.getId()))
                .isInstanceOf(GameRoomHandler.class)
                .satisfies(exception -> {
                    GameRoomHandler handler = (GameRoomHandler) exception;
                    assertThat(handler.getCode()).isEqualTo(ErrorStatus.GAME_ROOM_OPERATION_IN_PROGRESS);
                });

        verify(lock).tryLock(5, 10, TimeUnit.SECONDS);
        verify(gameRoomRedisService, never()).removePlayerFromRoom(anyString(), anyLong());
        verify(gameRoomService, never()).leaveGameRoom(any(), any());
    }

    @Test
    @DisplayName("락 내부에서 Redis 상태 재검증 확인")
    void shouldRevalidateRedisStateInLock() throws InterruptedException {
        // given
        when(gameRoomAdaptor.queryById(anyLong())).thenReturn(gameRoom);
        when(gameRoom.isNotHost(hostMember)).thenReturn(false);
        when(lock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);

        GameRoomPlayerInfo hostInfo = GameRoomPlayerInfo.builder()
                .memberId(hostMember.getId())
                .nickname(hostMember.getNickname())
                .isHost(true)
                .joinedAt(System.currentTimeMillis())
                .build();

        GameRoomPlayerInfo nextHost = GameRoomPlayerInfo.builder()
                .memberId(playerMember.getId())
                .nickname(playerMember.getNickname())
                .joinedAt(System.currentTimeMillis() + 1000)
                .build();

        // 락 내부에서 조회되는 최신 상태
        List<GameRoomPlayerInfo> currentPlayers = Arrays.asList(hostInfo, nextHost);
        when(gameRoomRedisService.getRoomPlayers(roomId)).thenReturn(currentPlayers);
        when(gameRoomRedisService.removePlayerFromRoom(roomId, hostMember.getId()))
                .thenReturn(hostInfo);

        List<GameRoomPlayerInfo> remainingPlayers = Arrays.asList(nextHost);
        when(gameRoomRedisService.getRoomPlayers(roomId))
                .thenReturn(currentPlayers) // 첫 번째 호출 (재검증)
                .thenReturn(remainingPlayers); // 두 번째 호출 (방장 변경 확인)

        when(memberAdaptor.queryById(playerMember.getId())).thenReturn(playerMember);

        // when
        leaveGameRoomUseCase.execute(hostMember, gameRoom.getId());

        // then: 락 내부에서 Redis 상태를 재검증했는지 확인
        verify(gameRoomRedisService, times(2)).getRoomPlayers(roomId);
        // 첫 번째 호출: makeLeaveDecisionWithLock에서 재검증
        // 두 번째 호출: applyLeaveToRedis에서 방장 변경 확인
        verify(lock).unlock();
    }

    @Test
    @DisplayName("새 방장이 이미 퇴장한 경우 Fallback 처리")
    void shouldHandleNewHostAlreadyLeft() throws InterruptedException {
        // given
        when(gameRoomAdaptor.queryById(anyLong())).thenReturn(gameRoom);
        when(gameRoom.isNotHost(hostMember)).thenReturn(false);
        when(lock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);

        GameRoomPlayerInfo hostInfo = GameRoomPlayerInfo.builder()
                .memberId(hostMember.getId())
                .nickname(hostMember.getNickname())
                .isHost(true)
                .joinedAt(System.currentTimeMillis())
                .build();

        GameRoomPlayerInfo nextHost = GameRoomPlayerInfo.builder()
                .memberId(playerMember.getId())
                .nickname(playerMember.getNickname())
                .joinedAt(System.currentTimeMillis() + 1000)
                .build();

        // 락 내부에서 조회 시 nextHost가 있음
        List<GameRoomPlayerInfo> currentPlayers = Arrays.asList(hostInfo, nextHost);
        when(gameRoomRedisService.getRoomPlayers(roomId))
                .thenReturn(currentPlayers) // 첫 번째 호출 (재검증)
                .thenReturn(Arrays.asList()); // 두 번째 호출 (nextHost가 이미 퇴장함)

        when(gameRoomRedisService.removePlayerFromRoom(roomId, hostMember.getId()))
                .thenReturn(hostInfo);

        // when
        leaveGameRoomUseCase.execute(hostMember, gameRoom.getId());

        // then: Fallback으로 방 삭제
        verify(gameRoomRedisService).deleteRoomData(roomId);
        verify(gameRoomService).deleteRoom(gameRoom);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        GameRoomLeaveEvent event = eventCaptor.getValue();
        // 실제로는 CHANGE_HOST로 결정되었지만, Fallback으로 방 삭제됨
        verify(lock).unlock();
    }

    @Test
    @DisplayName("InterruptedException 처리")
    void shouldHandleInterruptedException() throws InterruptedException {
        // given
        when(gameRoomAdaptor.queryById(anyLong())).thenReturn(gameRoom);
        when(lock.tryLock(5, 10, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

        // when & then
        assertThatThrownBy(() -> leaveGameRoomUseCase.execute(playerMember, gameRoom.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Lock acquisition interrupted");

        verify(lock, never()).unlock();
    }

    @Test
    @DisplayName("락 해제 보장 - finally 블록 확인")
    void shouldAlwaysUnlockInFinally() throws InterruptedException {
        // given
        when(gameRoomAdaptor.queryById(anyLong())).thenReturn(gameRoom);
        when(gameRoom.isNotHost(playerMember)).thenReturn(true);
        when(lock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.builder()
                .memberId(playerMember.getId())
                .build();
        when(gameRoomRedisService.removePlayerFromRoom(roomId, playerMember.getId()))
                .thenReturn(playerInfo);

        // when
        leaveGameRoomUseCase.execute(playerMember, gameRoom.getId());

        // then: finally 블록에서도 unlock 호출 확인
        verify(lock, atLeastOnce()).unlock();
    }
}
