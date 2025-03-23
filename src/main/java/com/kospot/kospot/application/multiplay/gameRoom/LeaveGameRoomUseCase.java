package com.kospot.kospot.application.multiplay.gameRoom;

import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.multiplay.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.kospot.domain.multiplay.gameRoom.entity.GameRoom;
import com.kospot.kospot.domain.multiplay.gameRoom.service.GameRoomService;
import com.kospot.kospot.global.annotation.usecase.UseCase;
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
        GameRoom gameRoom = gameRoomAdaptor.queryByIdFetchPlayers(gameRoomId);
        gameRoomService.leaveGameRoom(player, gameRoom);
    }

}
