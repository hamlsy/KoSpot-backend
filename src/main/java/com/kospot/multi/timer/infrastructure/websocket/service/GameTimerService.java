package com.kospot.multi.timer.infrastructure.websocket.service;

import com.kospot.multi.timer.application.message.RoundTransitionTimerMessage;
import com.kospot.multi.timer.application.message.TimerStartMessage;
import com.kospot.multi.timer.application.message.TimerSyncMessage;
import com.kospot.multi.game.domain.entity.MultiGame;
import com.kospot.multi.round.entity.BaseGameRound;
import com.kospot.multi.game.infrastructure.websocket.constants.MultiGameChannelConstants;
import com.kospot.multi.common.event.RoundCompletionEvent;
import com.kospot.multi.timer.entity.vo.TimerCommand;
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

    // Outer Map Key: gameRoomId
    // Inner Map Key: taskKey
    private final Map<String, Map<String, ScheduledFuture<?>>> roomTaskMap = new ConcurrentHashMap<>();

    // ==========================================
    //  Task Key Constants
    // ==========================================

    /** 라운드 진행 시간 동기화 태스크 접두어 (예: "sync:101") */
    private static final String KEY_PREFIX_ROUND_SYNC = "sync:";

    /** 라운드 종료 처리 태스크 접두어 (예: "complete:101") */
    private static final String KEY_PREFIX_ROUND_COMPLETE = "complete:";

    /** 라운드 전환(Transition) 그룹 접두어 (예: "transition:500") */
    private static final String KEY_PREFIX_TRANSITION = "transition:";

    /** 전환: 다음 라운드로 넘어가는 메인 로직 태스크 접미어 (예: "...:main") */
    private static final String KEY_SUFFIX_TRANSITION_MAIN = ":main";

    /** 전환: 대기 시간 동안 클라이언트와 시간 동기화 태스크 접미어 (예: "...:sync") */
    private static final String KEY_SUFFIX_TRANSITION_SYNC = ":sync";


    // ==========================================
    // Configuration Constants
    // ==========================================
    private static final int SYNC_INTERVAL_MS = 5000;
    private static final int FINAL_COUNTDOWN_THRESHOLD_MS = 10000;
    private static final int NEXT_ROUND_DELAY_SECONDS = 10;
    private static final int TRANSITION_SYNC_INTERVAL_MS = 2000;

    public GameTimerService(SimpMessagingTemplate messagingTemplate,
                            ApplicationEventPublisher eventPublisher,
                            @Qualifier("gameTimerTaskScheduler") TaskScheduler gameTimerTaskScheduler) {
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
        this.gameTimerTaskScheduler = gameTimerTaskScheduler;
    }

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

        scheduleTimerSync(command);
        scheduleRoundCompletion(command);
    }

    private void scheduleTimerSync(TimerCommand command) {
        String gameRoomId = command.getGameRoomId();
        BaseGameRound round = command.getRound();
        String taskKey = KEY_PREFIX_ROUND_SYNC + round.getId();

        cancelTask(gameRoomId, taskKey);

        ScheduledFuture<?> syncTask = gameTimerTaskScheduler.scheduleAtFixedRate(() -> {
            long remainingTimeMs = round.getRemainingTimeMs();
            if (remainingTimeMs <= 0) {
                cancelTask(gameRoomId, taskKey);
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

        }, Instant.now(), Duration.ofMillis(SYNC_INTERVAL_MS));

        registerTask(gameRoomId, taskKey, syncTask);
    }

    private void scheduleRoundCompletion(TimerCommand command) {
        String gameRoomId = command.getGameRoomId();
        BaseGameRound round = command.getRound();
        Long gameId = command.getGameId();
        String taskKey = KEY_PREFIX_ROUND_COMPLETE + round.getId();

        Instant completionTime = round.getServerStartTime().plus(round.getDuration());

        ScheduledFuture<?> completionTask = gameTimerTaskScheduler.schedule(() -> {
            try {
                RoundCompletionEvent event = new RoundCompletionEvent(gameRoomId, gameId, round.getId(), command.getGameMode(), command.getMatchType());
                eventPublisher.publishEvent(event);

                //라운드 종료 시 관련 태스크 정리
                cancelTask(gameRoomId, KEY_PREFIX_ROUND_SYNC + round.getId());
                cancelTask(gameRoomId, KEY_PREFIX_ROUND_COMPLETE + round.getId());

            } catch (Exception e) {
                log.error("Round completion error - GameRoomId: {}", gameRoomId, e);
            }
        }, completionTime);

        registerTask(gameRoomId, taskKey, completionTask);
    }

    public void startRoundTransitionTimer(String gameRoomId, MultiGame game, Runnable onTransitionComplete) {
        String baseKey = KEY_PREFIX_TRANSITION + game.getId();
        String mainTaskKey = baseKey + KEY_SUFFIX_TRANSITION_MAIN;
        String syncTaskKey = baseKey + KEY_SUFFIX_TRANSITION_SYNC;

        Instant transitionStartTime = Instant.now();
        Instant transitionEndTime = transitionStartTime.plusSeconds(NEXT_ROUND_DELAY_SECONDS);

        cancelTask(gameRoomId, mainTaskKey);
        cancelTask(gameRoomId, syncTaskKey);

        broadcastRoundTransitionTimer(gameRoomId, game, transitionEndTime);

        ScheduledFuture<?> syncTask = gameTimerTaskScheduler.scheduleAtFixedRate(() -> {
            long remainingMs = transitionEndTime.toEpochMilli() - System.currentTimeMillis();
            if (remainingMs <= 0) {
                cancelTask(gameRoomId, syncTaskKey);
                return;
            }
            broadcastRoundTransitionTimer(gameRoomId, game, transitionEndTime);
        }, Instant.now().plusMillis(TRANSITION_SYNC_INTERVAL_MS), Duration.ofMillis(TRANSITION_SYNC_INTERVAL_MS));

        registerTask(gameRoomId, syncTaskKey, syncTask);

        ScheduledFuture<?> transitionTask = gameTimerTaskScheduler.schedule(() -> {
            try {
                onTransitionComplete.run();
                log.info("Round transition completed - RoomId: {}", gameRoomId);
            } catch (Exception e) {
                log.error("Round transition failed - RoomId: {}", gameRoomId, e);
            } finally {
                cancelTask(gameRoomId, mainTaskKey);
                cancelTask(gameRoomId, syncTaskKey);
            }
        }, transitionEndTime);

        registerTask(gameRoomId, mainTaskKey, transitionTask);
    }

    private void broadcastRoundTransitionTimer(String gameRoomId, MultiGame game, Instant fixedNextRoundStartTime) {
        RoundTransitionTimerMessage message = RoundTransitionTimerMessage.builder()
                .nextRoundStartTimeMs(fixedNextRoundStartTime.toEpochMilli())
                .serverTimestamp(System.currentTimeMillis())
                .isLastRound(game.isLastRound())
                .build();
        messagingTemplate.convertAndSend(MultiGameChannelConstants.getRoundTransitionChannel(gameRoomId), message);
    }

    public void stopRoundTimer(String gameRoomId, BaseGameRound round) {
        cancelTask(gameRoomId, KEY_PREFIX_ROUND_SYNC + round.getId());
        cancelTask(gameRoomId, KEY_PREFIX_ROUND_COMPLETE + round.getId());
    }

    public void cancelAllForGame(String gameRoomId, Long gameId) {
        Map<String, ScheduledFuture<?>> tasks = roomTaskMap.remove(gameRoomId);

        if (tasks != null) {
            tasks.values().forEach(task -> {
                if (task != null) task.cancel(false);
            });
            log.info("All timer tasks cancelled for game - RoomId: {}, TaskCount: {}", gameRoomId, tasks.size());
        } else {
            log.info("No active timer tasks found for game - RoomId: {}", gameRoomId);
        }
    }

    private void registerTask(String gameRoomId, String taskKey, ScheduledFuture<?> task) {
        roomTaskMap.computeIfAbsent(gameRoomId, k -> new ConcurrentHashMap<>())
                .put(taskKey, task);
    }

    private void cancelTask(String gameRoomId, String taskKey) {
        Map<String, ScheduledFuture<?>> tasks = roomTaskMap.get(gameRoomId);
        if (tasks != null) {
            ScheduledFuture<?> task = tasks.remove(taskKey);
            if (task != null) {
                task.cancel(false);
            }
        }
    }
}