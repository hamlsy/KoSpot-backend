package com.kospot.infrastructure.websocket.domain.multi.timer.service;

import com.kospot.application.multiplayer.timer.message.TimerStartMessage;
import com.kospot.application.multiplayer.timer.message.TimerSyncMessage;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.infrastructure.redis.domain.multi.round.dao.GameRoundRedisRepository;
import com.kospot.infrastructure.redis.domain.multi.timer.dao.GameTimerRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class GameTimerService {

    private final GameRoundRedisRepository gameRoundRedisRepository;
    private final GameTimerRedisRepository gameTimerRedisRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskScheduler taskScheduler;

    private static final int SYNC_INTERVAL_MS = 5000; // 5초마다 동기화
    private static final int FINAL_COUNTDOWN_THRESHOLD_MS = 10000; // 마지막 10초 카운트다운

    private static final String TIMER_START_SUFFIX = "/timer/start";

    private String getTimerTopic(String gameId) {
        return String.format("/game/%s/timer/sync", gameId);
    }

    /**
     * 라운드 시작
     * - Redis 저장으로 서버 재시작 시에도 타이머 유지
     */

    public void startRoundTimer(String gameId, String roundId, GameMode mode, Set<String> playerIds) {
        Instant serverStartTime = Instant.now();

        TimerStartMessage message = TimerStartMessage.builder()
                .roundId(roundId)
                .gameMode(mode)
                .serverStartTimeMs(serverStartTime.toEpochMilli())
                .durationMs(mode.getDuration().toMillis())
                .serverTimestamp(System.currentTimeMillis())
                .build();

        broadcastToGame(gameId, TIMER_START_SUFFIX, message);

        // 타이머 스케줄링
        // scheduleTimerSync(gameId);
        // scheduleRondCompletion(round);

    }

    private void broadcastToGame(String gameId, String suffix, Object message) {
        String destination = "/topic/game/" + gameId + suffix;
        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * 주기적 타이머 동기화 (5초마다)
     */
     private void scheduleTimerSync(String gameId) {
        String topic = getTimerTopic(gameId);

        ScheduledFuture<?> syncTask = taskScheduler.scheduleAtFixedRate(() -> {
            //todo implement timerSyncMessage
            // params: roundId, remainingTimeMs, serverTimestamp, isFinalCountDown
            // redis 에서 roundId로 round 정보 가져오기

            TimerSyncMessage message = TimerSyncMessage.builder()
                    .roundId()
                    .serverTimestamp()
                    .remainingTimeMs()
                    .isFinalCountDown()
                    .build();
            broadcastToGame(gameId, topic, message);
        }, Instant.now().plusMillis(SYNC_INTERVAL_MS), Duration.ofMillis(SYNC_INTERVAL_MS)); // 현재 시각 기준으로 5초 뒤에 첫 실행, 이후 5초 간격으로 계속 반복

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
    private void scheduleRoundCompletion() {
        //todo implement
    }}

}
