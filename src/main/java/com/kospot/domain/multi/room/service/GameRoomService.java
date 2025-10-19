package com.kospot.domain.multi.room.service;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.repository.GameRoomRepository;
import com.kospot.domain.multi.room.vo.GameRoomUpdateInfo;
import com.kospot.infrastructure.exception.object.domain.WebSocketHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.presentation.multi.gameroom.dto.request.GameRoomRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GameRoomService {

    private final MemberAdaptor memberAdaptor;
    private final GameRoomRepository gameRoomRepository;

    public GameRoom createGameRoom(Member host, GameRoomRequest.Create request) {
        GameRoom gameRoom = request.toEntity();
        gameRoom.setHost(host);
        return gameRoomRepository.save(gameRoom);
    }

    public GameRoom updateGameRoom(GameRoomUpdateInfo updateInfo, GameRoom gameRoom) {
        gameRoom.update(updateInfo.getTitle(), GameMode.fromKey(updateInfo.getGameModeKey()), PlayerMatchType.fromKey(updateInfo.getPlayerMatchTypeKey()),
                updateInfo.isPrivateRoom(), updateInfo.getPassword(), updateInfo.getTeamCount());
        return gameRoom;
    }

    public void joinGameRoom(Member player, GameRoom gameRoom, GameRoomRequest.Join request) {
        gameRoom.join(player, request.getPassword());
    }

    public void leaveGameRoom(Member player, GameRoom gameRoom) {
        gameRoom.leaveRoom(player);
    }

    public void deleteRoom(GameRoom gameRoom) {
        gameRoom.deleteRoom();
        memberAdaptor.queryAllByGameRoomId(gameRoom.getId()).forEach(Member::leaveGameRoom);
    }

    public void kickPlayer(Member host, Member targetPlayer, GameRoom gameRoom) {
        gameRoom.kickPlayer(host, targetPlayer);
    }

    public void updateMemberLeaveStatus(Member member) {
        member.leaveGameRoom();
    }

    public void validateKickPermission(GameRoom gameRoom, Member host, Long targetMemberId) {
        gameRoom.validateHost(host);

        Member targetMember = memberAdaptor.queryById(targetMemberId);
        if (!targetMember.getGameRoomId().equals(gameRoom.getId())) {
            throw new WebSocketHandler(ErrorStatus._BAD_REQUEST);
        }
    }

    public void changeHostToMember(GameRoom gameRoom, Member newHost) {
        gameRoom.setHost(newHost);
    }
}
