package com.kospot.application.multiplayer.timer.service;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multigame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multigame.timer.vo.TimerData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleGameTimerService {

    private static final String TIMER_KEY_PREFIX = "timer:";
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

//    public void startRoadViewTimer(Long roomId, GameMode gameMode, RoadViewGameRound round) {
//        startTimer(roomId, gameMode, round.getRoundId());
//
//    }
//
//    public void startPhotoTimer() {
//
//    }

    public void startTimer(Long roomId, GameMode gameMode, String roundId) {
        // Redis TTL 설정
        String timerKey = String.format(
                TIMER_KEY_PREFIX + "%s:%s:%s",
                roomId, gameMode.name(), roundId
        );

        TimerData timerData = TimerData.builder()
                .roundId(roundId)
                .durationSeconds(6000)
                .startTimeMillis(System.currentTimeMillis())
                .build();


        // TTL을 게임 시간으로 설정
        redisTemplate.opsForValue().set(
                timerKey,
                timerData,
                Duration.ofSeconds(6000)
        );

//        // 3. 클라이언트들에게 타이머 시작 알림
        notifyTimerStart(round);
    }

    /**
     * 남은 시간 조회
     */
//    public TimerResponse getRemainingTime(String roundId) {
//        return null;
//    }

    /**
     * 강제로 타이머 종료
     */
//    public void stopTimer(String roundId) {}

    /**
     * TTL 만료 처리
     */
//    public void handleTimerExpiration(String expiredKey) {}

    private void notifyTimerStart() {

    }

}
