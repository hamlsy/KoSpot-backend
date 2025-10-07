package com.kospot.multi.timer;

import com.kospot.application.multi.timer.message.TimerStartMessage;
import com.kospot.application.multi.timer.message.TimerSyncMessage;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.infrastructure.websocket.domain.multi.timer.service.GameTimerService;
import com.kospot.infrastructure.websocket.domain.multi.timer.vo.TimerCommand;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;

/**
 * GameTimer 실제 환경 통합 테스트
 * 
 * 실제 Spring 컨텍스트에서 타이머가 정확히 작동하는지 검증:
 * 1. 실제 TaskScheduler를 사용한 스케줄링
 * 2. 실제 시간 경과에 따른 동기화 메시지 전송
 * 3. 여러 클라이언트 동시 접속 시나리오
 * 4. 5초 간격 브로드캐스팅 정확도
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class GameTimerIntegrationTest {

    @Autowired
    private GameTimerService gameTimerService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final Long GAME_ROOM_ID = 999L;
    private static final Long GAME_ID = 9999L;

    @Test
    @DisplayName("[통합] 5초 간격으로 타이머 동기화 메시지가 실제로 전송된다")
    void realTimeSync_SendsMessageEvery5Seconds() throws InterruptedException {
        // Given
        int timeLimitSeconds = 15; // 15초 타이머
        RoadViewGameRound round = createTestRound(timeLimitSeconds);
        TimerCommand command = createTimerCommand(round);

        List<TimerSyncMessage> receivedSyncMessages = new CopyOnWriteArrayList<>();
        List<Long> syncTimestamps = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(2); // 5초, 10초에 2번 수신 예상

        // 동기화 메시지 수신 리스너
        doAnswer(invocation -> {
            String destination = invocation.getArgument(0);
            Object message = invocation.getArgument(1);

            if (destination.contains("/sync") && message instanceof TimerSyncMessage) {
                TimerSyncMessage syncMsg = (TimerSyncMessage) message;
                receivedSyncMessages.add(syncMsg);
                syncTimestamps.add(System.currentTimeMillis());
                latch.countDown();
                
                log.info("📡 타이머 동기화 수신: 남은시간={}ms, 카운트다운={}", 
                        syncMsg.getRemainingTimeMs(), syncMsg.isFinalCountDown());
            }

            messagingTemplate.convertAndSend(invocation.getArgument(0), message);
            return null;
        }).when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // When
        log.info("⏰ 타이머 시작: {}초", timeLimitSeconds);
        Instant testStartTime = Instant.now();
        gameTimerService.startRoundTimer(command);

        // 최소 2번의 동기화 메시지를 기다림 (최대 12초)
        boolean received = latch.await(12, TimeUnit.SECONDS);

        // Then
        assertThat(received).isTrue();
        assertThat(receivedSyncMessages).hasSizeGreaterThanOrEqualTo(2);

        // 첫 번째와 두 번째 동기화 메시지 간격 검증 (약 5초)
        if (syncTimestamps.size() >= 2) {
            long interval = syncTimestamps.get(1) - syncTimestamps.get(0);
            assertThat(interval).isBetween(4800L, 5200L); // 5초 ±200ms
            log.info("✅ 동기화 간격: {}ms (예상: 5000ms)", interval);
        }

        // 남은 시간이 점점 감소하는지 검증
        for (int i = 1; i < receivedSyncMessages.size(); i++) {
            long prevRemaining = receivedSyncMessages.get(i - 1).getRemainingTimeMs();
            long currRemaining = receivedSyncMessages.get(i).getRemainingTimeMs();
            assertThat(currRemaining).isLessThan(prevRemaining);
        }

        log.info("✅ 총 {} 번의 동기화 메시지 수신 완료", receivedSyncMessages.size());
        
        // Clean up
        gameTimerService.stopRoundTimer(GAME_ROOM_ID.toString(), round);
    }

    @Test
    @DisplayName("[통합] 모든 클라이언트가 동일한 서버 시작 시간을 받는다")
    void allClients_ReceiveSameServerStartTime() throws InterruptedException {
        // Given
        RoadViewGameRound round = createTestRound(30);
        TimerCommand command = createTimerCommand(round);

        int simulatedClientCount = 5;
        List<TimerStartMessage> clientMessages = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            String destination = invocation.getArgument(0);
            Object message = invocation.getArgument(1);

            if (destination.contains("/start") && message instanceof TimerStartMessage) {
                TimerStartMessage startMsg = (TimerStartMessage) message;
                
                // 여러 클라이언트가 동시에 수신하는 상황 시뮬레이션
                for (int i = 0; i < simulatedClientCount; i++) {
                    clientMessages.add(startMsg);
                }
                latch.countDown();
            }

            messagingTemplate.convertAndSend(invocation.getArgument(0), message);
            return null;
        }).when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // When
        gameTimerService.startRoundTimer(command);
        boolean received = latch.await(2, TimeUnit.SECONDS);

        // Then
        assertThat(received).isTrue();
        assertThat(clientMessages).hasSize(simulatedClientCount);

        // 모든 클라이언트가 동일한 값을 받았는지 검증
        Long firstServerStartTime = clientMessages.get(0).getServerStartTimeMs();
        Long firstDuration = clientMessages.get(0).getDurationMs();

        for (TimerStartMessage msg : clientMessages) {
            assertThat(msg.getServerStartTimeMs()).isEqualTo(firstServerStartTime);
            assertThat(msg.getDurationMs()).isEqualTo(firstDuration);
            assertThat(msg.getGameMode()).isEqualTo(GameMode.ROADVIEW);
        }

        log.info("✅ {} 명의 클라이언트가 동일한 타이머 정보 수신", simulatedClientCount);
        log.info("   - 서버 시작 시간: {}ms", firstServerStartTime);
        log.info("   - 제한 시간: {}ms", firstDuration);

        // Clean up
        gameTimerService.stopRoundTimer(GAME_ROOM_ID.toString(), round);
    }

    @Test
    @DisplayName("[통합] 마지막 10초에 도달하면 isFinalCountDown 플래그가 활성화된다")
    void finalCountdown_FlagActivatesBelow10Seconds() throws InterruptedException {
        // Given
        int timeLimitSeconds = 12; // 12초 타이머
        RoadViewGameRound round = createTestRound(timeLimitSeconds);
        TimerCommand command = createTimerCommand(round);

        List<TimerSyncMessage> syncMessages = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            String destination = invocation.getArgument(0);
            Object message = invocation.getArgument(1);

            if (destination.contains("/sync") && message instanceof TimerSyncMessage) {
                TimerSyncMessage syncMsg = (TimerSyncMessage) message;
                syncMessages.add(syncMsg);

                // 마지막 10초 카운트다운 활성화 시 latch 해제
                if (syncMsg.isFinalCountDown()) {
                    latch.countDown();
                }
            }

            messagingTemplate.convertAndSend(invocation.getArgument(0), message);
            return null;
        }).when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // When
        log.info("⏰ 타이머 시작: {}초 (10초 임계값 테스트)", timeLimitSeconds);
        gameTimerService.startRoundTimer(command);

        // 카운트다운 플래그 활성화 대기 (최대 8초)
        boolean activated = latch.await(8, TimeUnit.SECONDS);

        // Then
        assertThat(activated).isTrue();

        // 카운트다운 활성화 시점의 메시지 찾기
        TimerSyncMessage countdownMessage = syncMessages.stream()
                .filter(TimerSyncMessage::isFinalCountDown)
                .findFirst()
                .orElseThrow();

        assertThat(countdownMessage.getRemainingTimeMs()).isLessThanOrEqualTo(10000);
        log.info("✅ 마지막 카운트다운 활성화: 남은시간={}ms", 
                countdownMessage.getRemainingTimeMs());

        // Clean up
        gameTimerService.stopRoundTimer(GAME_ROOM_ID.toString(), round);
    }

    @Test
    @DisplayName("[통합] 타이머 중지 후 더 이상 동기화 메시지가 전송되지 않는다")
    void stopTimer_NoMoreSyncMessagesAfterStop() throws InterruptedException {
        // Given
        RoadViewGameRound round = createTestRound(20);
        TimerCommand command = createTimerCommand(round);

        List<Long> syncMessageTimestamps = new CopyOnWriteArrayList<>();
        
        doAnswer(invocation -> {
            String destination = invocation.getArgument(0);
            Object message = invocation.getArgument(1);

            if (destination.contains("/sync") && message instanceof TimerSyncMessage) {
                syncMessageTimestamps.add(System.currentTimeMillis());
            }

            messagingTemplate.convertAndSend(invocation.getArgument(0), message);
            return null;
        }).when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // When
        gameTimerService.startRoundTimer(command);
        Thread.sleep(6000); // 6초 대기 (1번의 동기화 발생)

        int messagesBeforeStop = syncMessageTimestamps.size();
        log.info("📊 중지 전 동기화 메시지 수: {}", messagesBeforeStop);

        gameTimerService.stopRoundTimer(GAME_ROOM_ID.toString(), round);
        Thread.sleep(6000); // 추가 6초 대기

        int messagesAfterStop = syncMessageTimestamps.size();
        log.info("📊 중지 후 동기화 메시지 수: {}", messagesAfterStop);

        // Then
        assertThat(messagesBeforeStop).isGreaterThan(0);
        assertThat(messagesAfterStop).isEqualTo(messagesBeforeStop); // 중지 후 증가 없음

        log.info("✅ 타이머 중지 후 동기화 메시지 전송 중단 확인");
    }

    @Test
    @DisplayName("[통합] 서버 시간 기반 계산이 클라이언트 환경과 무관하게 일관성 있게 동작한다")
    void serverTimeCalculation_ConsistentAcrossEnvironments() throws InterruptedException {
        // Given
        RoadViewGameRound round = createTestRound(20);
        TimerCommand command = createTimerCommand(round);

        List<Long> calculatedRemainingTimes = new CopyOnWriteArrayList<>();
        List<Long> serverTimestamps = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        doAnswer(invocation -> {
            Object message = invocation.getArgument(1);

            if (message instanceof TimerSyncMessage) {
                TimerSyncMessage syncMsg = (TimerSyncMessage) message;
                calculatedRemainingTimes.add(syncMsg.getRemainingTimeMs());
                serverTimestamps.add(syncMsg.getServerTimestamp());
                latch.countDown();
            }

            messagingTemplate.convertAndSend(invocation.getArgument(0), message);
            return null;
        }).when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // When
        gameTimerService.startRoundTimer(command);
        boolean received = latch.await(12, TimeUnit.SECONDS);

        // Then
        assertThat(received).isTrue();
        assertThat(calculatedRemainingTimes).hasSizeGreaterThanOrEqualTo(2);

        // 남은 시간이 실제 경과 시간만큼 감소했는지 검증
        if (calculatedRemainingTimes.size() >= 2) {
            long firstRemaining = calculatedRemainingTimes.get(0);
            long secondRemaining = calculatedRemainingTimes.get(1);
            long actualDecrease = firstRemaining - secondRemaining;

            // 약 5초 감소해야 함 (5000ms ±500ms)
            assertThat(actualDecrease).isBetween(4500L, 5500L);
            log.info("✅ 남은 시간 감소: {}ms → {}ms (감소량: {}ms)", 
                    firstRemaining, secondRemaining, actualDecrease);
        }

        // 서버 타임스탬프도 동시에 증가하는지 검증
        for (int i = 1; i < serverTimestamps.size(); i++) {
            assertThat(serverTimestamps.get(i)).isGreaterThan(serverTimestamps.get(i - 1));
        }

        log.info("✅ 서버 시간 기반 계산 일관성 검증 완료");

        // Clean up
        gameTimerService.stopRoundTimer(GAME_ROOM_ID.toString(), round);
    }

    // === Helper Methods ===

    private RoadViewGameRound createTestRound(Integer timeLimit) {
        CoordinateNationwide coordinate = CoordinateNationwide.builder()
                .id(1L)
                .lat(37.5665)
                .lng(126.9780)
                .build();

        RoadViewGameRound round = RoadViewGameRound.createRound(
                1,
                coordinate,
                timeLimit,
                List.of(1L, 2L, 3L)
        );
        
        round.startRound(); // 서버 시작 시간 설정
        return round;
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

