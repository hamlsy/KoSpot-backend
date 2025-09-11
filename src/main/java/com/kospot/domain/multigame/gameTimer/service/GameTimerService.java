package com.kospot.domain.multigame.gameTimer.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GameTimerService {
    //todo refactoring redis
    private static final long ROUND_DURATION_MS = 2 * 60 * 1000; // 2분

    @Getter
    private long remainingTimeMs = ROUND_DURATION_MS;

    @Getter
    private boolean isRunning = false;

    public void startRound() {
        remainingTimeMs = ROUND_DURATION_MS;
        isRunning = true;
    }

    public void endRound() {
        isRunning = false;
        remainingTimeMs = 0;
    }

    @Scheduled(fixedRate = 1000)
    public void updateTimer() {
        if (!isRunning) return;

        remainingTimeMs -= 1000;

        if (remainingTimeMs <= 0) {
            endRound();
        }
    }


}
