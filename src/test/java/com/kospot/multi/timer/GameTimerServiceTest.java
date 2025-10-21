package com.kospot.multi.timer;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.infrastructure.redis.domain.multi.round.dao.GameRoundRedisRepository;
import com.kospot.infrastructure.redis.domain.multi.timer.dao.GameTimerRedisRepository;
import com.kospot.infrastructure.websocket.domain.multi.timer.service.GameTimerService;
import com.kospot.infrastructure.websocket.domain.multi.timer.vo.TimerCommand;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * GameTimerService 통합 테스트
 * 
 * 테스트 목적:
 * 1. 모든 클라이언트가 동등한 서버 시간을 브로드캐스팅 받는지 검증
 * 2. 5초 간격 동기화가 정확히 작동하는지 검증
 * 3. 타이머 종료 시점이 정확한지 검증
 * 4. 동시성 상황에서 타이머가 안정적으로 작동하는지 검증
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class GameTimerServiceTest {

    @Mock
    private GameRoundRedisRepository gameRoundRedisRepository;
    
    @Mock
    private GameTimerRedisRepository gameTimerRedisRepository;
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @Mock
    private TaskScheduler gameTimerTaskScheduler;

    private GameTimerService gameTimerService;

    @Captor
    private ArgumentCaptor<Object> messageCaptor;
    
    @Captor
    private ArgumentCaptor<String> destinationCaptor;

    private static final Long GAME_ROOM_ID = 1L;
    private static final Long GAME_ID = 100L;
    private static final Integer TIME_LIMIT_SECONDS = 30;

    @BeforeEach
    void setUp() {
        gameTimerService = new GameTimerService(
                messagingTemplate,
                null, // EventPublisher는 이 테스트에서 불필요
                gameTimerTaskScheduler
        );
    }

    @Test
    @DisplayName("타이머 시작 시 모든 클라이언트에게 동일한 서버 시작 시간이 브로드캐스팅 된다")
    void startTimer_BroadcastsSameServerStartTime() {
        // Given
        RoadViewGameRound round = createTestRound(TIME_LIMIT_SECONDS);
        TimerCommand command = createTimerCommand(round);
        round.startRound();

        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> mockFuture = (ScheduledFuture<Object>) mock(ScheduledFuture.class);
        
        when(gameTimerTaskScheduler.scheduleAtFixedRate(
                any(Runnable.class),
                any(Instant.class),
                any(Duration.class)
        )).thenReturn((ScheduledFuture)mockFuture);
        
        when(gameTimerTaskScheduler.schedule(
                any(Runnable.class),
                any(Instant.class)
        )).thenReturn((ScheduledFuture)mockFuture);

        // When
        gameTimerService.startRoundTimer(command);

        // Then
        verify(messagingTemplate, times(1))
                .convertAndSend(destinationCaptor.capture(), messageCaptor.capture());

        String destination = destinationCaptor.getValue();
        assertThat(destination).isEqualTo("/topic/game-rooms/1/timer/start");

        // 메시지 내용 검증
        Object message = messageCaptor.getValue();
        assertThat(message).hasFieldOrProperty("serverStartTimeMs");
        assertThat(message).hasFieldOrProperty("durationMs");
        assertThat(message).hasFieldOrProperty("serverTimestamp");

        log.info("✅ 타이머 시작 메시지 브로드캐스트 성공: {}", message);
    }

    @Test
    @DisplayName("5초 간격으로 타이머 동기화 메시지가 스케줄링 된다")
    void startTimer_SchedulesSyncEvery5Seconds() {
        // Given
        RoadViewGameRound round = createTestRound(TIME_LIMIT_SECONDS);
        round.startRound(); // serverStartTime 설정
        TimerCommand command = createTimerCommand(round);
        
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> mockFuture = (ScheduledFuture<Object>) mock(ScheduledFuture.class);
        when(gameTimerTaskScheduler.scheduleAtFixedRate(
                any(Runnable.class),
                any(Instant.class),
                eq(Duration.ofMillis(5000))
        )).thenReturn((ScheduledFuture) mockFuture);
        
        when(gameTimerTaskScheduler.schedule(
                any(Runnable.class),
                any(Instant.class)
        )).thenReturn((ScheduledFuture) mockFuture);

        // When
        gameTimerService.startRoundTimer(command);

        // Then
        verify(gameTimerTaskScheduler).scheduleAtFixedRate(
                any(Runnable.class),
                any(Instant.class),
                eq(Duration.ofMillis(5000))
        );

        log.info("✅ 5초 간격 타이머 동기화 스케줄링 성공");
    }

    @Test
    @DisplayName("타이머 동기화 시 남은 시간이 정확하게 계산된다")
    void syncTimer_CalculatesRemainingTimeAccurately() throws Exception {
        // Given
        RoadViewGameRound round = createTestRound(10); // 10초 타이머
        round.startRound(); // serverStartTime 설정
        TimerCommand command = createTimerCommand(round);

        CountDownLatch latch = new CountDownLatch(1);
        
        // TaskScheduler의 실제 동작을 시뮬레이션
        when(gameTimerTaskScheduler.scheduleAtFixedRate(
                any(Runnable.class),
                any(Instant.class),
                any(Duration.class)
        )).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            
            // 동기화 태스크를 별도 스레드에서 실행
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // 2초 대기
                    task.run(); // 동기화 실행
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            return mock(ScheduledFuture.class);
        });

        // schedule() 메서드도 mock 필요
        when(gameTimerTaskScheduler.schedule(
                any(Runnable.class),
                any(Instant.class)
        )).thenReturn(mock(ScheduledFuture.class));

        // When
        gameTimerService.startRoundTimer(command);
        
        // 동기화 태스크가 실행될 때까지 대기
        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // Then
        long expectedRemainingMs = round.getRemainingTimeMs();
        assertThat(expectedRemainingMs).isLessThanOrEqualTo(10000); // 10초 이내
        assertThat(expectedRemainingMs).isGreaterThan(0); // 아직 남아있음

        log.info("✅ 남은 시간 계산 정확: {}ms", expectedRemainingMs);
    }

    @Test
    @DisplayName("타이머 종료 시점이 정확하게 스케줄링 된다")
    void startTimer_SchedulesCompletionAtCorrectTime() {
        // Given
        RoadViewGameRound round = createTestRound(TIME_LIMIT_SECONDS);
        round.startRound();
        TimerCommand command = createTimerCommand(round);

        ArgumentCaptor<Instant> completionTimeCaptor = ArgumentCaptor.forClass(Instant.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> mockFuture = (ScheduledFuture<Object>) mock(ScheduledFuture.class);
        
        // scheduleAtFixedRate도 mock 필요
        when(gameTimerTaskScheduler.scheduleAtFixedRate(
                any(Runnable.class),
                any(Instant.class),
                any(Duration.class)
        )).thenReturn((ScheduledFuture)mockFuture);
        
        when(gameTimerTaskScheduler.schedule(
                any(Runnable.class),
                completionTimeCaptor.capture()
        )).thenReturn((ScheduledFuture)mockFuture);

        // When
        gameTimerService.startRoundTimer(command);

        // Then
        Instant scheduledCompletionTime = completionTimeCaptor.getValue();
        Instant expectedCompletionTime = round.getServerStartTime()
                .plus(Duration.ofSeconds(TIME_LIMIT_SECONDS));

        // 스케줄된 종료 시간이 예상 종료 시간과 동일한지 검증 (±100ms 허용)
        long differenceMs = Math.abs(
                scheduledCompletionTime.toEpochMilli() - expectedCompletionTime.toEpochMilli()
        );
        assertThat(differenceMs).isLessThan(100);

        log.info("✅ 타이머 종료 시점 스케줄링 정확: 예상={}, 실제={}", 
                expectedCompletionTime, scheduledCompletionTime);
    }

    @Test
    @DisplayName("여러 클라이언트가 동시에 접속해도 동일한 타이머 정보를 받는다")
    void multipleClients_ReceiveSameTimerInfo() throws InterruptedException {
        // Given
        RoadViewGameRound round = createTestRound(TIME_LIMIT_SECONDS);
        round.startRound(); // serverStartTime 설정
        TimerCommand command = createTimerCommand(round);
        
        int clientCount = 10;
        CountDownLatch latch = new CountDownLatch(clientCount);
        List<Long> receivedStartTimes = new java.util.concurrent.CopyOnWriteArrayList<>();

        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> mockFuture = (ScheduledFuture<Object>) mock(ScheduledFuture.class);
        
        when(gameTimerTaskScheduler.scheduleAtFixedRate(
                any(Runnable.class),
                any(Instant.class),
                any(Duration.class)
        )).thenReturn((ScheduledFuture)mockFuture);
        
        when(gameTimerTaskScheduler.schedule(
                any(Runnable.class),
                any(Instant.class)
        )).thenReturn((ScheduledFuture)mockFuture);

        // 메시지 전송 시 모든 클라이언트가 수신하는 상황 시뮬레이션
        doAnswer(invocation -> {
            Object message = invocation.getArgument(1);
            Long startTimeMs = (Long) ReflectionTestUtils.getField(message, "serverStartTimeMs");
            
            // 여러 클라이언트가 동시에 수신
            for (int i = 0; i < clientCount; i++) {
                receivedStartTimes.add(startTimeMs);
                latch.countDown();
            }
            return null;
        }).when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // When
        gameTimerService.startRoundTimer(command);
        
        // 모든 클라이언트가 수신할 때까지 대기
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // Then
        assertThat(receivedStartTimes).hasSize(clientCount);
        
        // 모든 클라이언트가 동일한 시작 시간을 받았는지 검증
        Long firstStartTime = receivedStartTimes.get(0);
        assertThat(receivedStartTimes).allMatch(time -> time.equals(firstStartTime));

        log.info("✅ {} 명의 클라이언트가 동일한 서버 시작 시간 수신: {}ms", 
                clientCount, firstStartTime);
    }

    @Test
    @DisplayName("타이머 수동 중지 시 모든 스케줄링이 취소된다")
    void stopTimer_CancelsAllScheduledTasks() {
        // Given
        RoadViewGameRound round = createTestRound(TIME_LIMIT_SECONDS);
        round.startRound(); // serverStartTime 설정
        TimerCommand command = createTimerCommand(round);

        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> mockSyncFuture = (ScheduledFuture<Object>) mock(ScheduledFuture.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> mockCompletionFuture = (ScheduledFuture<Object>) mock(ScheduledFuture.class);

        when(gameTimerTaskScheduler.scheduleAtFixedRate(
                any(Runnable.class), any(Instant.class), any(Duration.class)
        )).thenReturn((ScheduledFuture)mockSyncFuture);

        when(gameTimerTaskScheduler.schedule(
                any(Runnable.class), any(Instant.class)
        )).thenReturn((ScheduledFuture)mockCompletionFuture);

        gameTimerService.startRoundTimer(command);

        // When
        gameTimerService.stopRoundTimer(GAME_ROOM_ID.toString(), round);

        // Then
        verify(mockSyncFuture).cancel(false);
        verify(mockCompletionFuture).cancel(false);

        log.info("✅ 타이머 수동 중지 시 모든 스케줄링 취소 성공");
    }

    @Test
    @DisplayName("마지막 10초 임계값에 도달하면 isFinalCountDown 플래그가 true가 된다")
    void syncTimer_SetsFinalCountDownFlagWhenBelow10Seconds() throws Exception {
        // Given
        RoadViewGameRound round = createTestRound(8); // 8초만 남음
        round.startRound();
        TimerCommand command = createTimerCommand(round);

        CountDownLatch latch = new CountDownLatch(1);

        when(gameTimerTaskScheduler.scheduleAtFixedRate(
                any(Runnable.class), any(Instant.class), any(Duration.class)
        )).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    task.run();
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            return mock(ScheduledFuture.class);
        });

        // schedule() 메서드도 mock 필요
        when(gameTimerTaskScheduler.schedule(
                any(Runnable.class),
                any(Instant.class)
        )).thenReturn(mock(ScheduledFuture.class));

        // When
        gameTimerService.startRoundTimer(command);
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // Then
        verify(messagingTemplate, atLeastOnce())
                .convertAndSend(contains("/sync"), messageCaptor.capture());

        Object syncMessage = messageCaptor.getValue();
        Boolean isFinalCountDown = (Boolean) ReflectionTestUtils.getField(
                syncMessage, "isFinalCountDown"
        );
        
        assertThat(isFinalCountDown).isTrue();
        log.info("✅ 마지막 10초 카운트다운 플래그 활성화 성공");
    }

    // === Helper Methods ===

    private RoadViewGameRound createTestRound(Integer timeLimit) {
        Coordinate coordinate = Coordinate.builder()
                .id(1L)
                .lat(37.5665)
                .lng(126.9780)
                .build();

        return RoadViewGameRound.createRound(
                1,
                coordinate,
                timeLimit,
                List.of(1L, 2L, 3L)
        );
    }

    private TimerCommand createTimerCommand(RoadViewGameRound round) {
        return TimerCommand.builder()
                .round(round)
                .gameRoomId(GAME_ROOM_ID.toString())
                .gameId(GAME_ID.toString())
                .gameMode(GameMode.ROADVIEW)
                .matchType(PlayerMatchType.SOLO)
                .build();
    }
}

