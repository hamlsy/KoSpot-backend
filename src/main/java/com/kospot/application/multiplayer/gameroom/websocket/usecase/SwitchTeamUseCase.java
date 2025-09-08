package com.kospot.application.multiplayer.gameroom.websocket.usecase;

import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class SwitchTeamUseCase {

    private final GameRoomNotificationService gameRoomNotificationService;

    public void execute(String roomId, Long memberId, String newTeam) {
        gameRoomNotificationService.notify(roomId, memberId, newTeam);
        log.info("Player switched team - MemberId: {}, RoomId: {}, NewTeam: {}", memberId, roomId, newTeam);
    }

}
