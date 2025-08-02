package com.kospot.domain.multigame.gameRoom.service;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multigame.game.vo.PlayerMatchType;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.repository.GameRoomRepository;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomPlayerService;
import com.kospot.presentation.multigame.gameroom.dto.request.GameRoomRequest;
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
    private final GameRoomPlayerService gameRoomPlayerService;

    public GameRoom createGameRoom(Member host, GameRoomRequest.Create request) {
        GameRoom gameRoom = request.toEntity();
        gameRoom.setHost(host);
        return gameRoomRepository.save(gameRoom);
    }

    public GameRoom updateGameRoom(Member host, GameRoomRequest.Update request, GameRoom gameRoom) {
        gameRoom.validateHost(host);
        gameRoom.update(request.getTitle(), GameMode.fromKey(request.getGameModeKey()), PlayerMatchType.fromKey(request.getPlayerMatchTypeKey()),
                request.isPrivateRoom(), request.getPassword(), request.getTeamCount());
        return gameRoom;
    }

    public void joinGameRoom(Member player, GameRoom gameRoom, GameRoomRequest.Join request) {
        gameRoom.join(player, request.getPassword());
    }

    public void leaveGameRoom(Member player, GameRoom gameRoom) {
        //플레이어가 나간 경우
        gameRoom.leaveRoom(player);

        //방장이 나간 경우 또는 남은 플레이어가 없는 경우 (Redis 기반 실시간 확인)
        if (gameRoom.isHost(player) || gameRoomPlayerService.isRoomEmpty(gameRoom.getId().toString())) {
            deleteRoom(gameRoom);
        }

    }

    // todo soft delete scheduling
    private void deleteRoom(GameRoom gameRoom) {
        gameRoom.deleteRoom();
        //남은 인원들 game room fk 초기화
        memberAdaptor.queryAllByGameRoomId(gameRoom.getId()).forEach(Member::leaveGameRoom);
    }

    public void kickPlayer(Member host, Member targetPlayer, GameRoom gameRoom) {
        gameRoom.kickPlayer(host, targetPlayer);
    }


}
