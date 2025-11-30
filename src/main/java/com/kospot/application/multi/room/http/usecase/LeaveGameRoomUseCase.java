package com.kospot.application.multi.room.http.usecase;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.event.GameRoomLeaveEvent;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.multi.room.adaptor.GameRoomRedisAdaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class LeaveGameRoomUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;
    private final GameRoomRedisAdaptor gameRoomRedisAdaptor;
    private final ApplicationEventPublisher eventPublisher;

    public void execute(Member member, Long gameRoomId) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        member.leaveGameRoom();

        // 나가는 사람이 방장일 경우 방장 교체 & 나 말고 플레이어가 있을 때
        // 나 제외 가장 최근에 들어온 사람
        Optional<GameRoomPlayerInfo> nextHostInfo =
                gameRoomRedisAdaptor.pickNextHostByJoinedAt(
                        gameRoomId.toString(), member.getId());


        // redis 방장 교체
        // todo 방장 교체 이벤트 발행

        // db 방장 교체

        // redis 퇴장 처리
        eventPublisher.publishEvent(new GameRoomLeaveEvent(gameRoom, member));

    }

}
