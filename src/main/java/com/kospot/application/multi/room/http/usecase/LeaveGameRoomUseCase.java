package com.kospot.application.multi.room.http.usecase;

import com.kospot.application.multi.room.vo.LeaveDecision;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.event.GameRoomLeaveEvent;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.multi.room.adaptor.GameRoomRedisAdaptor;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
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

        LeaveDecision decision = makeLeaveDecision(gameRoom, member);

        // todo db 상태 변경



        eventPublisher.publishEvent(new GameRoomLeaveEvent(gameRoom,
                member, decision));

    }

    private LeaveDecision makeLeaveDecision(GameRoom gameRoom, Member member) {
        String roomId = gameRoom.getId().toString();
        if(gameRoom.isNotHost(member)){
            return LeaveDecision.normalLeave(member);
        }

        // 방장 퇴장
        // 다음 방장 찾기
        Optional<GameRoomPlayerInfo> nextHostCandidate = gameRoomRedisAdaptor.pickNextHostByJoinedAt(
                roomId, member.getId());

        // 다음 방장이 없으면 방 삭제
        if (nextHostCandidate.isEmpty()) {
            return LeaveDecision.deleteRoom(gameRoom, member);
        }

        // 방장 교체
        return LeaveDecision.changeHost(
                gameRoom,
                member,
                nextHostCandidate.get()
        );

    }

}
