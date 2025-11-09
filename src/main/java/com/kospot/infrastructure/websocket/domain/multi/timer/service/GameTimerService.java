package com.kospot.infrastructure.websocket.domain.multi.timer.service;

import com.kospot.application.multi.timer.message.RoundTransitionTimerMessage;
import com.kospot.application.multi.timer.message.TimerStartMessage;
import com.kospot.application.multi.timer.message.TimerSyncMessage;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.entity.MultiGame;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.round.entity.BaseGameRound;
import com.kospot.infrastructure.websocket.domain.multi.game.constants.MultiGameChannelConstants;
import com.kospot.domain.multi.timer.event.RoundCompletionEvent;
import com.kospot.infrastructure.websocket.domain.multi.timer.vo.TimerCommand;
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
    private final Map<String, ScheduledFuture<?>> transitionTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> transitionSyncTasks = new ConcurrentHashMap<>();

    private static final int SYNC_INTERVAL_MS = 5000; // 5초마다 동기화
    private static final int FINAL_COUNTDOWN_THRESHOLD_MS = 10000; // 마지막 10초 카운트다운
    private static final int NEXT_ROUND_DELAY_SECONDS = 10; // 라운드 전환 대기 시간
    private static final int TRANSITION_SYNC_INTERVAL_MS = 2000; // 전환 대기 중 동기화 (2초)

    public GameTimerService(SimpMessagingTemplate messagingTemplate,
                            ApplicationEventPublisher eventPublisher,
                            @Qualifier("gameTimerTaskScheduler") TaskScheduler gameTimerTaskScheduler) {
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
        this.gameTimerTaskScheduler = gameTimerTaskScheduler;
    }

    /**
     * 라운드 시작
     */
    public void startRoundTimer(TimerCommand command) {
        Instant serverStartTime = Instant.now();
        String gameRoomId = command.getGameRoomId();
        BaseGameRound round = command.getRound();
        TimerStartMessage startMessage = TimerStartMessage.builder()
                .roundId(round.getId().toString())
                .gameMode(round.getGameMode().name())
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
                    .roundId(round.getId().toString())
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
        return gameRoomId + ":" + round.getId();
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
        Long gameId = command.getGameId();

        String taskKey = getTaskKey(gameRoomId, round);
        Instant completionTime = round.getServerStartTime().plus(round.getDuration());
        ScheduledFuture<?> completionTask = gameTimerTaskScheduler.schedule(() -> {
            try {
                RoundCompletionEvent event = new RoundCompletionEvent(gameRoomId, gameId, round.getId(), gameMode, matchType);
                eventPublisher.publishEvent(event);

                // Task 정리
                cancelAllTasks(taskKey);

            } catch (Exception e) {
                log.error("Round completion error - GameRoomId: {}, RoundId: {}", gameRoomId, round.getId(), e);
            }
        }, completionTime);
        completionTasks.put(taskKey, completionTask);
    }

    /**
     * 라운드 전환 대기 타이머 브로드캐스트
     * - 라운드 결과 표시 후 다음 라운드까지 10초 대기
     */
    public void startRoundTransitionTimer(String gameRoomId, MultiGame game,
                                          Runnable onTransitionComplete) {
        String taskKey = getTransitionTaskKey(gameRoomId, game.getId());
        Instant transitionStartTime = Instant.now();
        Instant transitionEndTime = transitionStartTime.plusSeconds(NEXT_ROUND_DELAY_SECONDS);

        // 기존 전환 태스크 취소 (중복 방지)
        cancelTransitionTask(taskKey);
        cancelTransitionSyncTask(taskKey);

        // 1. 초기 전환 타이머 브로드캐스트
        broadcastRoundTransitionTimer(gameRoomId, game);

        // 2. 2초마다 동기화 브로드캐스트 스케줄링
        scheduleTransitionSync(gameRoomId, game, transitionEndTime, taskKey);

        // 3. 10초 후 콜백 실행 스케줄링
        scheduleRoundTransitionCallBack(gameRoomId, game.getId(), onTransitionComplete, transitionEndTime, taskKey);

    }

    /**
     * 라운드 전환 대기 중 2초마다 동기화
     */
    private void scheduleTransitionSync(String gameRoomId, MultiGame game,
                                        Instant transitionEndTime, String taskKey) {
        ScheduledFuture<?> syncTask = gameTimerTaskScheduler.scheduleAtFixedRate(() -> {
                    long remainingMs = transitionEndTime.toEpochMilli() - System.currentTimeMillis();

                    if (remainingMs <= 0) {
                        cancelTransitionSyncTask(taskKey);
                        return;
                    }

                    broadcastRoundTransitionTimer(gameRoomId, game);

                }, Instant.now().plusMillis(TRANSITION_SYNC_INTERVAL_MS),
                Duration.ofMillis(TRANSITION_SYNC_INTERVAL_MS));

        transitionSyncTasks.put(taskKey, syncTask);

        log.debug("Transition sync scheduled - RoomId: {}, Interval: {}ms",
                gameRoomId, TRANSITION_SYNC_INTERVAL_MS);
    }

    private void broadcastRoundTransitionTimer(String gameRoomId, MultiGame game) {
        Instant now = Instant.now();
        Instant nextRoundStartTime = now.plusSeconds(NEXT_ROUND_DELAY_SECONDS);

        RoundTransitionTimerMessage message = RoundTransitionTimerMessage.builder()
                .nextRoundStartTimeMs(nextRoundStartTime.toEpochMilli())
                .serverTimestamp(System.currentTimeMillis())
                .isLastRound(game.isLastRound())
                .build();

        String destination = MultiGameChannelConstants.getRoundTransitionChannel(gameRoomId);
        messagingTemplate.convertAndSend(destination, message);

        log.info("Round transition timer broadcasted - RoomId: {}, NextStartTime: {}, IsLastRound: {}",
                gameRoomId, nextRoundStartTime, game.isLastRound());
    }

    /**
     * 라운드 전환 콜백 스케줄링
     */
    private void scheduleRoundTransitionCallBack(String gameRoomId, Long gameId,
                                                 Runnable onComplete,
                                                 Instant executeTime, String taskKey) {
        ScheduledFuture<?> transitionTask = gameTimerTaskScheduler.schedule(() -> {
            try {
                onComplete.run();
                log.info("✅ Round transition completed - RoomId: {}, GameId: {}", gameRoomId, gameId);
            } catch (Exception e) {
                log.error("🚨 Round transition failed - RoomId: {}, GameId: {}", gameRoomId, gameId, e);
            } finally {
                cancelTransitionTask(taskKey);
                cancelTransitionSyncTask(taskKey);
            }
        }, executeTime);

        transitionTasks.put(taskKey, transitionTask);
    }

    private static String getTransitionTaskKey(String gameRoomId, Long gameId) {
        return "transition:" + gameRoomId + ":" + gameId;
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

    private void cancelTransitionTask(String taskKey) {
        ScheduledFuture<?> transitionTask = transitionTasks.remove(taskKey);
        if (transitionTask != null) {
            transitionTask.cancel(false);
            log.debug("Transition task cancelled - TaskKey: {}", taskKey);
        }
    }

    private void cancelTransitionSyncTask(String taskKey) {
        ScheduledFuture<?> syncTask = transitionSyncTasks.remove(taskKey);
        if (syncTask != null) {
            syncTask.cancel(false);
            log.debug("Transition sync task cancelled - TaskKey: {}", taskKey);
        }
    }
}
