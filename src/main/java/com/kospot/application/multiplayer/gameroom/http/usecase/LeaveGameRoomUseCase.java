package com.kospot.application.multiplayer.gameroom.http.usecase;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multigame.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multigame.room.entity.GameRoom;
import com.kospot.domain.multigame.room.event.GameRoomLeaveEvent;
import com.kospot.domain.multigame.room.service.GameRoomService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class LeaveGameRoomUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;
    private final ApplicationEventPublisher eventPublisher;

    public void execute(Member member, Long gameRoomId) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        
        // 데이터베이스 레벨에서 퇴장 처리
        gameRoomService.leaveGameRoom(member, gameRoom);

        eventPublisher.publishEvent(new GameRoomLeaveEvent(gameRoom, member));

    }

}
