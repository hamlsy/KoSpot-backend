package com.kospot.multi.room.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.multi.game.domain.vo.PlayerMatchType;
import com.kospot.multi.room.application.adaptor.GameRoomAdaptor;
import com.kospot.multi.room.domain.entity.GameRoom;
import com.kospot.multi.room.application.service.service.GameRoomService;
import com.kospot.multi.room.domain.vo.GameRoomUpdateInfo;
import com.kospot.common.annotation.usecase.UseCase;

import com.kospot.multi.lobby.infrastructure.websocket.service.LobbyRoomNotificationService;
import com.kospot.multi.room.infrastructure.redis.adaptor.GameRoomRedisAdaptor;
import com.kospot.multi.room.infrastructure.redis.service.GameRoomRedisService;
import com.kospot.multi.room.infrastructure.websocket.service.GameRoomNotificationService;

import com.kospot.multi.room.presentation.dto.request.GameRoomRequest;
import com.kospot.multi.room.presentation.dto.response.GameRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class UpdateGameRoomSettingsUseCase {

    private final MemberAdaptor memberAdaptor;
    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;
    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomRedisAdaptor gameRoomRedisAdaptor;

    // notify
    private final GameRoomNotificationService gameRoomNotificationService;
    private final LobbyRoomNotificationService lobbyRoomNotificationService;

    // todo refactor
    public GameRoomResponse execute(Long hostId, GameRoomRequest.Update request, Long gameRoomId) {
        Member host = memberAdaptor.queryById(hostId);
        // host 검증
        GameRoom gameRoom = gameRoomAdaptor.queryByIdFetchHost(gameRoomId);
        gameRoom.validateHost(host);

        // 팀 설정 변경 처리 (업데이트 전 현재 상태와 비교)
        handleTeamSettingChanged(gameRoom, request);

        GameRoomUpdateInfo updateInfo = mapToUpdateInfo(request);
        GameRoom updatedGameRoom = gameRoomService.updateGameRoom(updateInfo, gameRoom);

        // notify
        gameRoomNotificationService.notifyRoomSettingsChanged(gameRoomId.toString(), updateInfo);
        lobbyRoomNotificationService.notifyRoomUpdated(updatedGameRoom, gameRoomRedisAdaptor.getCurrentPlayersCount(gameRoomId.toString()));

        return GameRoomResponse.from(updatedGameRoom);
    }

    public GameRoomUpdateInfo mapToUpdateInfo(GameRoomRequest.Update request) {
        return GameRoomUpdateInfo.builder()
                .title(request.getTitle())
                .gameModeKey(request.getGameModeKey())
                .playerMatchTypeKey(request.getPlayerMatchTypeKey())
                .privateRoom(request.isPrivateRoom())
                .totalRounds(request.getTotalRounds())
                .timeLimit(request.getTimeLimit())
                .poiNameVisible(request.isPoiNameVisible())
                .maxPlayers(request.getMaxPlayers())
                .password(request.getPassword())
                .build();
    }

    //todo refactor
    private void handleTeamSettingChanged(GameRoom gameRoom, GameRoomRequest.Update request) {
        PlayerMatchType currentPlayerMatchType = gameRoom.getPlayerMatchType();
        PlayerMatchType requestPlayerMatchType = PlayerMatchType.fromKey(request.getPlayerMatchTypeKey());

        if (currentPlayerMatchType != requestPlayerMatchType) { // 팀 변경 됐을 때
            String gameRoomId = gameRoom.getId().toString();

            switch (requestPlayerMatchType) {
                case SOLO -> gameRoomRedisService.resetAllPlayersTeam(gameRoomId);
                case TEAM -> gameRoomRedisService.assignAllPlayersTeam(gameRoomId);
            }
            // 2. playerList broadcast
            gameRoomNotificationService.notifyPlayerListUpdated(gameRoomId);
        }
    }


}
