package com.kospot.infrastructure.redis.domain.friend.chatstream.worker;

import com.kospot.domain.friend.model.FriendChatStreamMessage;
import com.kospot.infrastructure.redis.domain.friend.chatstream.config.FriendChatPersistProperties;
import com.kospot.infrastructure.redis.domain.friend.chatstream.dlq.FriendChatDlqPublisher;
import com.kospot.infrastructure.redis.domain.friend.chatstream.init.FriendChatStreamInitializer;
import com.kospot.infrastructure.redis.domain.friend.chatstream.persistence.FriendChatBatchInsertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendChatPersistWorker implements SmartLifecycle {

    private final StringRedisTemplate stringRedisTemplate;
    private final FriendChatPersistProperties properties;
    private final FriendChatBatchInsertRepository batchInsertRepository;
    private final FriendChatDlqPublisher dlqPublisher;
    private final FriendChatStreamInitializer streamInitializer;

    private final List<MapRecord<String, Object, Object>> buffer = new ArrayList<>();
    private final Map<String, Integer> failureCountByStreamId = new ConcurrentHashMap<>();

    private volatile boolean running;
    private ExecutorService executorService;
    private Future<?> workerFuture;
    private long lastFlushAt;
    private long lastNoGroupErrorLoggedAt;

    @Override
    public synchronized void start() {
        if (running || !properties.isEnabled()) {
            return;
        }

        ensureGroupWithRetryLog();

        running = true;
        lastFlushAt = System.currentTimeMillis();
        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "friend-chat-persist-worker");
            thread.setDaemon(true);
            return thread;
        });
        workerFuture = executorService.submit(this::runLoop);
        log.info("Friend chat persist worker started. streamKey={}, group={}, consumer={}",
                properties.getStreamKey(), properties.getGroup(), properties.getConsumerName());
    }

    private void runLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                List<MapRecord<String, Object, Object>> newRecords = readNewRecords();
                if (!newRecords.isEmpty()) {
                    buffer.addAll(newRecords);
                } else if (buffer.isEmpty()) {
                    List<MapRecord<String, Object, Object>> pendingRecords = readPendingRecords();
                    if (!pendingRecords.isEmpty()) {
                        buffer.addAll(pendingRecords);
                    }
                }

                if (shouldFlush()) {
                    flush();
                }
            } catch (Exception e) {
                if (isInterruptedError(e)) {
                    if (!running || Thread.currentThread().isInterrupted()) {
                        log.debug("Friend chat persist worker interrupted during shutdown.");
                        break;
                    }
                    log.debug("Friend chat persist worker read interrupted. waiting for next loop.");
                    continue;
                }
                if (isNoGroupError(e)) {
                    handleNoGroupError(e);
                    continue;
                }
                log.error("Friend chat persist worker loop error", e);
            }
        }

        try {
            flush();
        } catch (Exception e) {
            log.error("Friend chat persist worker final flush failed", e);
        }
    }

    private List<MapRecord<String, Object, Object>> readNewRecords() {
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                Consumer.from(properties.getGroup(), properties.getConsumerName()),
                StreamReadOptions.empty()
                        .count(properties.getReadCount())
                        .block(Duration.ofMillis(properties.getReadBlockMs())),
                StreamOffset.create(properties.getStreamKey(), ReadOffset.lastConsumed())
        );
        return records == null ? List.of() : records;
    }

    private List<MapRecord<String, Object, Object>> readPendingRecords() {
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                Consumer.from(properties.getGroup(), properties.getConsumerName()),
                StreamReadOptions.empty().count(properties.getReadCount()),
                StreamOffset.create(properties.getStreamKey(), ReadOffset.from("0-0"))
        );
        return records == null ? List.of() : records;
    }

    private boolean shouldFlush() {
        if (buffer.isEmpty()) {
            return false;
        }
        if (buffer.size() >= properties.getBatchSize()) {
            return true;
        }
        return System.currentTimeMillis() - lastFlushAt >= properties.getFlushIntervalMs();
    }

    private void flush() {
        if (buffer.isEmpty()) {
            return;
        }

        List<MapRecord<String, Object, Object>> flushTargets = new ArrayList<>(buffer);
        buffer.clear();

        List<FriendChatStreamMessage> payloads = new ArrayList<>(flushTargets.size());
        List<MapRecord<String, Object, Object>> validRecords = new ArrayList<>(flushTargets.size());

        for (MapRecord<String, Object, Object> record : flushTargets) {
            try {
                payloads.add(toPayload(record));
                validRecords.add(record);
            } catch (Exception e) {
                handleParseFailure(record, e);
            }
        }

        if (validRecords.isEmpty()) {
            lastFlushAt = System.currentTimeMillis();
            return;
        }

        try {
            batchInsertRepository.batchInsert(payloads);
            ackRecords(validRecords);
            validRecords.forEach(record -> failureCountByStreamId.remove(record.getId().getValue()));
            log.debug("Friend chat batch persisted. size={}", validRecords.size());
        } catch (Exception e) {
            handleBatchFailure(validRecords, e);
        } finally {
            lastFlushAt = System.currentTimeMillis();
        }
    }

    private FriendChatStreamMessage toPayload(MapRecord<String, Object, Object> record) {
        Map<String, String> values = new HashMap<>();
        record.getValue().forEach((k, v) -> values.put(String.valueOf(k), String.valueOf(v)));

        String messageId = require(values, "message_id");
        Long roomId = Long.parseLong(require(values, "room_id"));
        Long senderMemberId = Long.parseLong(require(values, "sender_member_id"));
        String content = require(values, "content");
        LocalDateTime createdAt = LocalDateTime.parse(require(values, "created_at"));
        int retryCount = Integer.parseInt(values.getOrDefault("retry_count", "0"));

        return new FriendChatStreamMessage(messageId, roomId, senderMemberId, content, createdAt, retryCount);
    }

    private String require(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing stream field: " + key);
        }
        return value;
    }

    private void handleParseFailure(MapRecord<String, Object, Object> record, Exception e) {
        try {
            dlqPublisher.publish(record, "PAYLOAD_PARSE_FAILED:" + e.getClass().getSimpleName());
            ackRecords(List.of(record));
        } catch (Exception dlqError) {
            log.error("Failed to publish parse failure message to DLQ. streamId={}", record.getId().getValue(), dlqError);
        }
    }

    private void handleBatchFailure(List<MapRecord<String, Object, Object>> records, Exception e) {
        log.warn("Friend chat batch insert failed. size={}, reason={}", records.size(), e.getMessage());

        for (MapRecord<String, Object, Object> record : records) {
            String streamId = record.getId().getValue();
            int currentFailCount = failureCountByStreamId.merge(streamId, 1, Integer::sum);
            if (currentFailCount > properties.getMaxRetry()) {
                try {
                    dlqPublisher.publish(record, "DB_BATCH_INSERT_FAILED:" + e.getClass().getSimpleName());
                    ackRecords(List.of(record));
                    failureCountByStreamId.remove(streamId);
                } catch (Exception dlqError) {
                    log.error("Failed to publish batch failure message to DLQ. streamId={}", streamId, dlqError);
                }
            }
        }
    }

    private void ackRecords(List<MapRecord<String, Object, Object>> records) {
        if (records.isEmpty()) {
            return;
        }

        String[] ids = records.stream()
                .map(record -> record.getId().getValue())
                .toArray(String[]::new);

        stringRedisTemplate.opsForStream().acknowledge(properties.getStreamKey(), properties.getGroup(), ids);
    }

    private void ensureGroupWithRetryLog() {
        try {
            streamInitializer.ensureGroup();
        } catch (Exception e) {
            log.warn("Friend chat stream group initialization failed. worker will retry. streamKey={}, group={}, reason={}",
                    properties.getStreamKey(), properties.getGroup(), e.getMessage());
        }
    }

    private void handleNoGroupError(Exception e) {
        long now = System.currentTimeMillis();
        if (now - lastNoGroupErrorLoggedAt >= properties.getErrorLogThrottleMs()) {
            lastNoGroupErrorLoggedAt = now;
            log.warn("Friend chat stream/group missing. reinitializing. streamKey={}, group={}, reason={}",
                    properties.getStreamKey(), properties.getGroup(), e.getMessage());
        }

        ensureGroupWithRetryLog();
        sleepRetryDelay();
    }

    private boolean isNoGroupError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("NOGROUP")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isInterruptedError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InterruptedException) {
                return true;
            }
            String className = current.getClass().getName();
            if (className.endsWith("RedisCommandInterruptedException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void sleepRetryDelay() {
        try {
            Thread.sleep(properties.getInitRetryDelayMs());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }

        running = false;

        if (workerFuture != null) {
            workerFuture.cancel(true);
        }

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(properties.getShutdownTimeoutMs(), TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executorService.shutdownNow();
            }
        }

        log.info("Friend chat persist worker stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
