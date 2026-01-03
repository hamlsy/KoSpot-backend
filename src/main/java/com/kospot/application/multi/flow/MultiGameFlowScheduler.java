package com.kospot.application.multi.flow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
public class MultiGameFlowScheduler {

    private final TaskScheduler gameFlowTaskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public MultiGameFlowScheduler(@Qualifier("gameFlowTaskScheduler") TaskScheduler gameFlowTaskScheduler) {
        this.gameFlowTaskScheduler = gameFlowTaskScheduler;
    }

    public void schedule(String roomId, FlowTaskType taskType, Duration delay, Runnable task) {
        String taskKey = buildTaskKey(roomId, taskType);
        cancel(roomId, taskType);

        ScheduledFuture<?> future = gameFlowTaskScheduler.schedule(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Flow task execution failed - RoomId: {}, TaskType: {}", roomId, taskType, e);
            } finally {
                scheduledTasks.remove(taskKey);
            }
        }, Instant.now().plus(delay));

        if (future != null) {
            scheduledTasks.put(taskKey, future);
        }
    }

    public void cancel(String roomId, FlowTaskType taskType) {
        String taskKey = buildTaskKey(roomId, taskType);
        ScheduledFuture<?> future = scheduledTasks.remove(taskKey);
        if (future != null) {
            future.cancel(false);

        }
    }

    public void cancelAll(String roomId) {
        for (FlowTaskType taskType : FlowTaskType.values()) {
            cancel(roomId, taskType);
        }
    }

    private String buildTaskKey(String roomId, FlowTaskType taskType) {
        return roomId + ":" + taskType.name();
    }

    public enum FlowTaskType {
        COUNTDOWN,
        LOADING_TIMEOUT,
        INTRO
    }
}

