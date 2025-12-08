package com.kospot.application.multi.access.service;

import com.kospot.application.multi.room.http.usecase.GetGameRoomDetailUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.presentation.multi.access.dto.response.GameAccessResponse;
import com.kospot.presentation.multi.room.dto.response.GameRoomDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GameAccessService {

    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomAdaptor gameRoomAdaptor;

    private final GetGameRoomDetailUseCase getGameRoomDetailUseCase;

    public GameAccessResponse checkAccess(Member member, String roomId) {
        // 방 존재 확인
        if(isRoomNotAccessible(member, roomId)) {
            return GameAccessResponse.notAllowed("존재하지 않는 방입니다.");
        }

        // 게임 방 참여자가 아닌 경우
        // 1. redis 검증
        List<GameRoomPlayerInfo> gameRoomPlayerInfos =  gameRoomRedisService.getRoomPlayers(roomId);
        boolean isParticipant = gameRoomPlayerInfos.stream()
                .anyMatch(playerInfo -> playerInfo.getMemberId().equals(member.getId()));
        // 2. DB 검증
        boolean isParticipantInDB = member.getGameRoomId() != null &&
                member.getGameRoomId().equals(Long.parseLong(roomId));
        if(!isParticipant || !isParticipantInDB) {
            return GameAccessResponse.notAllowed("게임 방 참여자가 아닙니다.");
        }


        // 게임방 상태 확인 todo refactor, 일단 재접속 로직은 나중에 구현
        GameRoom room = gameRoomAdaptor.queryById(Long.parseLong(roomId));
        if(room.getStatus() != GameRoomStatus.WAITING) {
            return GameAccessResponse.notAllowed("잘못된 접근입니다.");
        }

        // 게임 방 참여자인 경우 게임 정보 반환
        GameRoomDetailResponse gameRoomDetailResponse = getGameRoomDetailUseCase.execute(Long.parseLong(roomId));
        return GameAccessResponse.allowed(gameRoomDetailResponse);
    }

    private boolean isRoomNotAccessible(Member member, String roomId) {
        if(roomId == null) {
            return true;
        }
        try {
            long id = Long.parseLong(roomId);
            if (id < 0) {
                return true;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        if(!gameRoomAdaptor.existsById(Long.parseLong(roomId))) {
            return true;
        }
        return false;
    }

}
