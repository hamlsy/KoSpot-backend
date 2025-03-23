package com.kospot.kospot.application.multiplay.gameRoom;

import com.kospot.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.multiplay.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.kospot.domain.multiplay.gameRoom.entity.GameRoom;
import com.kospot.kospot.domain.multiplay.gameRoom.service.GameRoomService;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import com.kospot.kospot.presentation.multiplay.gameRoom.dto.request.GameRoomRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class KickPlayerUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;
    private final MemberAdaptor memberAdaptor;

    public void execute(Member host, GameRoomRequest.Kick request, Long gameRoomId) {
        GameRoom gameRoom = gameRoomAdaptor.queryByIdFetchPlayers(gameRoomId);
        Member targetPlayer = memberAdaptor.queryById(request.getTargetPlayerId());
        gameRoomService.kickPlayer(host, targetPlayer, gameRoom);
    }

}
