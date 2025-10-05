package com.kospot.infrastructure.websocket.domain.multi.timer.service;

import com.kospot.application.multi.timer.message.TimerStartMessage;
import com.kospot.application.multi.timer.message.TimerSyncMessage;
import com.kospot.domain.multi.round.entity.BaseGameRound;
import com.kospot.infrastructure.redis.domain.multi.round.dao.GameRoundRedisRepository;
import com.kospot.infrastructure.redis.domain.multi.timer.dao.GameTimerRedisRepository;
import com.kospot.infrastructure.websocket.domain.multi.game.constants.MultiGameChannelConstants;
import com.kospot.infrastructure.websocket.domain.multi.timer.event.RoundCompletionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameTimerService {

    private final GameRoundRedisRepository gameRoundRedisRepository;
    private final GameTimerRedisRepository gameTimerRedisRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskScheduler taskScheduler;

    private final Map<String, ScheduledFuture<?>> syncTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> completionTasks = new ConcurrentHashMap<>();

    private static final int SYNC_INTERVAL_MS = 5000; // 5초마다 동기화
    private static final int FINAL_COUNTDOWN_THRESHOLD_MS = 10000; // 마지막 10초 카운트다운

    /**
     * 라운드 시작
     * - Redis 저장으로 서버 재시작 시에도 타이머 유지
     */

    public void startRoundTimer(String gameRoomId, BaseGameRound round) {
        Instant serverStartTime = Instant.now();

        TimerStartMessage startMessage = TimerStartMessage.builder()
                .roundId(round.getRoundId())
                .gameMode(round.getGameMode())
                .serverStartTimeMs(serverStartTime.toEpochMilli())
                .durationMs(round.getDuration().toMillis())
                .serverTimestamp(System.currentTimeMillis())
                .build();

        String startChannel = MultiGameChannelConstants.getTimerChannel(gameRoomId) + "/start";
        messagingTemplate.convertAndSend(startChannel, startMessage);

//        broadcastToGame(gameRoomId, TIMER_START_SUFFIX, message);

        // 타이머 스케줄링
         scheduleTimerSync(gameRoomId, round);
        // scheduleRondCompletion(round);

    }

    //todo gameId -> gameRoomId
    private void broadcastToGame(String gameId, String suffix, Object message) {
        String destination = "/topic/game/" + gameId + suffix;
        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * 주기적 타이머 동기화 (5초마다)
     */
    private void scheduleTimerSync(String gameRoomId, BaseGameRound round) {
        String taskKey = getTaskKey(gameRoomId, round);
        cancelSyncTask(taskKey);
        ScheduledFuture<?> syncTask = taskScheduler.scheduleAtFixedRate(() -> {
            long remainingTimeMs = round.getRemainingTimeMs();
            if (remainingTimeMs <= 0) {
                cancelSyncTask(taskKey);
            }
            boolean isFinalCountdown = remainingTimeMs <= FINAL_COUNTDOWN_THRESHOLD_MS;

            TimerSyncMessage syncMessage = TimerSyncMessage.builder()
                    .roundId(round.getRoundId())
                    .remainingTimeMs(remainingTimeMs)
                    .serverTimestamp(System.currentTimeMillis())
                    .isFinalCountDown(isFinalCountdown)
                    .build();
            String syncChannel = MultiGameChannelConstants.getTimerChannel(gameRoomId) + "/sync";
            messagingTemplate.convertAndSend(syncChannel, syncMessage);

        }, Instant.now().plusMillis(SYNC_INTERVAL_MS), Duration.ofMillis(SYNC_INTERVAL_MS)); // 현재 시각 기준으로 5초 뒤에 첫 실행, 이후 5초 간격으로 계속 반복
        syncTasks.put(taskKey, syncTask);
    }

    private static String getTaskKey(String gameRoomId, BaseGameRound round) {
        return gameRoomId + ":" + round.getRoundId();
    }

    /**
     * 마지막 10초 고빈도 동기화 (1초마다)
     */
    private void scheduleFrequentSync() {
        //todo implement
    }

    /**
     * 라운드 종료 스케줄링
     */
    private void scheduleRoundCompletion(String gameRoomId, BaseGameRound round) {
        String taskKey = getTaskKey(gameRoomId, round);
        Instant completionTime = round.getServerStartTime().plus(round.getDuration());
        ScheduledFuture<?> completionTask = taskScheduler.schedule(() -> {
            try {
                RoundCompletionEvent event = new RoundCompletionEvent(gameRoomId, round.getRoundId());
                eventPublisher.publishEvent(event);

                // Task 정리
                cancelAllTasks(taskKey);

            } catch (Exception e) {
                log.error("Round completion error - GameRoomId: {}, RoundId: {}", gameRoomId, round.getRoundId(), e);
            }
        }, completionTime);
        completionTasks.put(taskKey, completionTask);
    }

    /**
     * 타이머 수동 중지
     */
    public void stopRoundTimer(String gameRoomId, BaseGameRound round) {
        String taskKey = getTaskKey(gameRoomId, round);
        cancelAllTasks(taskKey);
    }

    // === Task 관리 ===
    private void cancelSyncTask(String taskKey) {
        ScheduledFuture<?> syncTask = syncTasks.remove(taskKey);
        if (syncTask != null) {
            syncTask.cancel(false);
        }
    }

    private void cancelCompletionTask(String taskKey) {
        ScheduledFuture<?> completionTask = completionTasks.remove(taskKey);
        if (completionTask != null) {
            completionTask.cancel(false);
        }
    }

    private void cancelAllTasks(String taskKey) {
        cancelSyncTask(taskKey);
        cancelCompletionTask(taskKey);
    }
}
