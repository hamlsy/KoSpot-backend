package com.kospot.infrastructure.websocket.domain.gameTimer.service;

import com.kospot.application.multiplayer.timer.message.TimerStartMessage;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.infrastructure.redis.domain.timer.dao.GameTimerRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GameTimerService {

    private final GameTimerRedisRepository gameTimerRedisRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private static final int SYNC_INTERVAL_MS = 5000; // 5초마다 동기화
    private static final int FINAL_COUNTDOWN_THRESHOLD_MS = 10000; // 마지막 10초 카운트다운

    private static final String TIMER_START_SUFFIX = "/tier/start";

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
        // scheduleTimerSync(round);
        // scheduleRondCompletion(round);

    }

    private void broadcastToGame(String gameId, String suffix, Object message) {
        String destination = "/topic/game/" + gameId + suffix;
        messagingTemplate.convertAndSend(destination, message);
    }

}
