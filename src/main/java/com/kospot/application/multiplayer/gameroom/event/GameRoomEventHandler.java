package com.kospot.application.multiplayer.gameroom.event;

import com.kospot.domain.multigame.gameRoom.event.GameRoomJoinEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomEventHandler {

    @EventListener
    public void handleJoin(GameRoomJoinEvent event) {
        // redis 업데이트
        // 입장 알림
        //
    }

}
