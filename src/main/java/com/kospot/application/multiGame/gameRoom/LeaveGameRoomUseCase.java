package com.kospot.application.multiGame.gameRoom;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multiGame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.domain.multiGame.gameRoom.service.GameRoomService;
import com.kospot.global.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class LeaveGameRoomUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;

    public void execute(Member player, Long gameRoomId) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        gameRoomService.leaveGameRoom(player, gameRoom);
    }

}
