package com.kospot.infrastructure.websocket.domain.multi.timer.service;

import com.kospot.application.multi.timer.message.TimerStartMessage;
import com.kospot.application.multi.timer.message.TimerSyncMessage;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.round.entity.BaseGameRound;
import com.kospot.infrastructure.redis.domain.multi.round.dao.GameRoundRedisRepository;
import com.kospot.infrastructure.redis.domain.multi.timer.dao.GameTimerRedisRepository;
import com.kospot.infrastructure.websocket.domain.multi.game.constants.MultiGameChannelConstants;
import com.kospot.infrastructure.redis.domain.multi.timer.event.RoundCompletionEvent;
import com.kospot.infrastructure.websocket.domain.multi.timer.vo.TimerCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class GameTimerService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private final TaskScheduler gameTimerTaskScheduler;

    private final Map<String, ScheduledFuture<?>> syncTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> completionTasks = new ConcurrentHashMap<>();

    private static final int SYNC_INTERVAL_MS = 5000; // 5초마다 동기화
    private static final int FINAL_COUNTDOWN_THRESHOLD_MS = 10000; // 마지막 10초 카운트다운

    public GameTimerService(SimpMessagingTemplate messagingTemplate,
                            ApplicationEventPublisher eventPublisher,
                            @Qualifier("gameTimerTaskScheduler") TaskScheduler gameTimerTaskScheduler) {
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
        this.gameTimerTaskScheduler = gameTimerTaskScheduler;
    }

    /**
     * 라운드 시작
     * - Redis 저장으로 서버 재시작 시에도 타이머 유지
     */

    public void startRoundTimer(TimerCommand command) {
        Instant serverStartTime = Instant.now();
        String gameRoomId = command.getGameRoomId();
        BaseGameRound round = command.getRound();
        TimerStartMessage startMessage = TimerStartMessage.builder()
                .roundId(round.getRoundId())
                .gameMode(round.getGameMode())
                .serverStartTimeMs(serverStartTime.toEpochMilli())
                .durationMs(round.getDuration().toMillis())
                .serverTimestamp(System.currentTimeMillis())
                .build();

        String startChannel = MultiGameChannelConstants.getTimerChannel(gameRoomId) + "/start";
        messagingTemplate.convertAndSend(startChannel, startMessage);

        // 타이머 스케줄링
        scheduleTimerSync(command);
        scheduleRoundCompletion(command);

    }

    /**
     * 주기적 타이머 동기화 (5초마다)
     */
    private void scheduleTimerSync(TimerCommand command) {
        String gameRoomId = command.getGameRoomId();
        BaseGameRound round = command.getRound();
        String taskKey = getTaskKey(gameRoomId, round);
        cancelSyncTask(taskKey);
        ScheduledFuture<?> syncTask = gameTimerTaskScheduler.scheduleAtFixedRate(() -> {
            long remainingTimeMs = round.getRemainingTimeMs();
            if (remainingTimeMs <= 0) {
                cancelSyncTask(taskKey);
                return;
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

        }, Instant.now(), Duration.ofMillis(SYNC_INTERVAL_MS)); // 현재 시각 기준으로 5초 뒤에 첫 실행, 이후 5초 간격으로 계속 반복
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
    private void scheduleRoundCompletion(TimerCommand command) {
        String gameRoomId = command.getGameRoomId();
        BaseGameRound round = command.getRound();
        GameMode gameMode = command.getGameMode();
        PlayerMatchType matchType = command.getMatchType();
        String gameId = command.getGameId();

        String taskKey = getTaskKey(gameRoomId, round);
        Instant completionTime = round.getServerStartTime().plus(round.getDuration());
        ScheduledFuture<?> completionTask = gameTimerTaskScheduler.schedule(() -> {
            try {
                RoundCompletionEvent event = new RoundCompletionEvent(gameRoomId, gameId, round.getRoundId(), gameMode, matchType);
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
