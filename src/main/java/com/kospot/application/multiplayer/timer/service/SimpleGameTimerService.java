package com.kospot.application.multiplayer.timer.service;

import com.kospot.domain.multigame.gameRound.repository.PhotoGameRoundRepository;
import com.kospot.domain.multigame.gameRound.repository.RoadViewGameRoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleGameTimerService {

    private static final String TIMER_KEY_PREFIX = "game_timer:";
    private final SimpMessagingTemplate messagingTemplate;

    public void startTimer(String roundId) {
        // Redis TTL 설정
        String timerKey = TIMER_KEY_PREFIX + roundId;

//        TimerData timerData = new TimerData(
//                round.getGameId(),
//                roundId,
//                round.getDurationSeconds(),
//                System.currentTimeMillis(),
//                new ArrayList<>(round.getPlayerIds())
//        );
//
//        // TTL을 게임 시간으로 설정
//        redisTemplate.opsForValue().set(
//                timerKey,
//                timerData,
//                Duration.ofSeconds(round.getDurationSeconds())
//        );
//
//        // 3. 클라이언트들에게 타이머 시작 알림
//        notifyTimerStart(round);
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

}
