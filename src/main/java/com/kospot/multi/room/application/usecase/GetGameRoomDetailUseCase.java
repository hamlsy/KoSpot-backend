package com.kospot.multi.room.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.multi.room.application.adaptor.GameRoomAdaptor;
import com.kospot.multi.room.domain.entity.GameRoom;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import com.kospot.common.annotation.usecase.UseCase;

import com.kospot.multi.room.infrastructure.redis.service.GameRoomRedisService;
import com.kospot.multi.room.presentation.dto.response.GameRoomDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetGameRoomDetailUseCase {

    private final MemberAdaptor memberAdaptor;
    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomRedisService gameRoomRedisService;

    public GameRoomDetailResponse execute(Long memberId, Long gameRoomId) {
        Member member = memberAdaptor.queryById(memberId);
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(gameRoomId.toString());

        return GameRoomDetailResponse.from(gameRoom, member, players);
    }

}
