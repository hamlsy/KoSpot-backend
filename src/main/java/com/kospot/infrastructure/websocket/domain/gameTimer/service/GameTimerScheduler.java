package com.kospot.infrastructure.websocket.domain.gameTimer.service;

import com.kospot.infrastructure.redis.domain.timer.service.GameTimerService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameTimerScheduler {

    private final TaskScheduler taskScheduler;
    private final GameTimerService gameTimerService;
    private final GameTimerBroadcaster gameTimerBroadcaster;

    // roomId -> ScheduledFuture 관리 (나중에 취소 가능)

    //todo 모든 플레이어가 제출하면 타이머 삭제
    public void startRoomTimer(String roomId, long roundTimeMs) {
        //라운드 시작

        //기존 스케줄 있으면 취소

        //1초마다 실행되는 스케줄 등록


    }



}
