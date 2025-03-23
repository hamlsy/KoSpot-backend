package com.kospot.application.multiGame.gameRoom;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multiGame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.domain.multiGame.gameRoom.service.GameRoomService;
import com.kospot.global.annotation.usecase.UseCase;
import com.kospot.presentation.multiGame.gameRoom.dto.request.GameRoomRequest;
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
