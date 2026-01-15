package com.kospot.presentation.test;

import com.kospot.application.multi.timer.message.TimerSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 부하 테스트용 Mock Timer Controller
 * 
 * k6에서 호출하여 실제 GameTimerService와 동일한 5초 간격 타이머 메시지를 브로드캐스팅합니다.
 * 프로덕션 환경에서는 비활성화됩니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
// @RequiredArgsConstructor
@Profile("!prod")
public class LoadTestController {

    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler gameTimerTaskScheduler;

    public LoadTestController(SimpMessagingTemplate messagingTemplate,
            @Qualifier("gameTimerTaskScheduler") TaskScheduler gameTimerTaskScheduler) {
        this.messagingTemplate = messagingTemplate;
        this.gameTimerTaskScheduler = gameTimerTaskScheduler;
    }

    private static final int SYNC_INTERVAL_MS = 5000; // 5초마다 동기화

    private final Map<String, ScheduledFuture<?>> activeMockTimers = new ConcurrentHashMap<>();
    private final Map<String, Long> timerStartTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> timerDurations = new ConcurrentHashMap<>();

    /**
     * Mock 타이머 시작
     * 
     * @param roomId     테스트용 룸 ID
     * @param durationMs 타이머 지속 시간 (밀리초, 기본값 60초)
     * @return 시작 결과
     */
    @PostMapping("/timer/start")
    public ResponseEntity<Map<String, Object>> startMockTimer(
            @RequestParam("roomId") String roomId,
            @RequestParam(value = "durationMs", defaultValue = "60000") long durationMs) {

        // 기존 타이머가 있으면 중지
        stopMockTimerInternal(roomId);

        long startTime = System.currentTimeMillis();
        timerStartTimes.put(roomId, startTime);
        timerDurations.put(roomId, durationMs);

        // 5초마다 TimerSyncMessage 브로드캐스팅
        ScheduledFuture<?> syncTask = gameTimerTaskScheduler.scheduleAtFixedRate(() -> {
            long elapsed = System.currentTimeMillis() - timerStartTimes.get(roomId);
            long remaining = timerDurations.get(roomId) - elapsed;

            if (remaining <= 0) {
                stopMockTimerInternal(roomId);
                log.info("[LoadTest] Mock timer completed - RoomId: {}", roomId);
                return;
            }

            boolean isFinalCountdown = remaining <= 10000;

            TimerSyncMessage syncMessage = TimerSyncMessage.builder()
                    .roundId("mock-round-" + roomId)
                    .remainingTimeMs(remaining)
                    .serverTimestamp(System.currentTimeMillis())
                    .isFinalCountDown(isFinalCountdown)
                    .build();

            String syncChannel = "/topic/game/" + roomId + "/timer/sync";
            messagingTemplate.convertAndSend(syncChannel, syncMessage);

            log.info("[LoadTest] Timer sync broadcasted - RoomId: {}, Remaining: {}ms", roomId, remaining);

        }, Instant.now(), Duration.ofMillis(SYNC_INTERVAL_MS));

        activeMockTimers.put(roomId, syncTask);

        log.info("[LoadTest] Mock timer started - RoomId: {}, Duration: {}ms, SyncInterval: {}ms",
                roomId, durationMs, SYNC_INTERVAL_MS);

        return ResponseEntity.ok(Map.of(
                "status", "started",
                "roomId", roomId,
                "durationMs", durationMs,
                "syncIntervalMs", SYNC_INTERVAL_MS,
                "timerChannel", "/topic/game/" + roomId + "/timer/sync"));
    }

    /**
     * Mock 타이머 중지
     */
    @PostMapping("/timer/stop")
    public ResponseEntity<Map<String, Object>> stopMockTimer(@RequestParam("roomId") String roomId) {
        boolean stopped = stopMockTimerInternal(roomId);

        return ResponseEntity.ok(Map.of(
                "status", stopped ? "stopped" : "not_found",
                "roomId", roomId));
    }

    /**
     * 활성 타이머 목록 조회
     */
    @GetMapping("/timer/active")
    public ResponseEntity<Map<String, Object>> getActiveTimers() {
        return ResponseEntity.ok(Map.of(
                "activeTimers", activeMockTimers.keySet(),
                "count", activeMockTimers.size()));
    }

    /**
     * 모든 Mock 타이머 중지
     */
    @PostMapping("/timer/stop-all")
    public ResponseEntity<Map<String, Object>> stopAllMockTimers() {
        int count = activeMockTimers.size();
        activeMockTimers.keySet().forEach(this::stopMockTimerInternal);

        log.info("[LoadTest] All mock timers stopped - Count: {}", count);

        return ResponseEntity.ok(Map.of(
                "status", "all_stopped",
                "stoppedCount", count));
    }

    private boolean stopMockTimerInternal(String roomId) {
        ScheduledFuture<?> task = activeMockTimers.remove(roomId);
        timerStartTimes.remove(roomId);
        timerDurations.remove(roomId);

        if (task != null) {
            task.cancel(false);
            log.info("[LoadTest] Mock timer stopped - RoomId: {}", roomId);
            return true;
        }
        return false;
    }
}
