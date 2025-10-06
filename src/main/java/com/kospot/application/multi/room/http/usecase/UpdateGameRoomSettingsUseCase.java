package com.kospot.application.multi.room.http.usecase;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.domain.multi.room.vo.GameRoomUpdateInfo;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.multi.room.service.GameRoomNotificationService;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.presentation.multi.gameroom.dto.request.GameRoomRequest;
import com.kospot.presentation.multi.gameroom.dto.response.GameRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class UpdateGameRoomSettingsUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;
    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomNotificationService gameRoomNotificationService;

    // todo refactor
    public GameRoomResponse execute(Member host, GameRoomRequest.Update request, Long gameRoomId) {
        // host 검증
        GameRoom gameRoom = gameRoomAdaptor.queryByIdFetchHost(gameRoomId);
        gameRoom.validateHost(host);

        // 팀 설정 변경 처리 (업데이트 전 현재 상태와 비교)
        handleTeamSettingChanged(gameRoom, request);

        GameRoomUpdateInfo updateInfo = mapToUpdateInfo(request);
        GameRoom updatedGameRoom = gameRoomService.updateGameRoom(updateInfo, gameRoom);

        gameRoomNotificationService.notifyRoomSettingsChanged(gameRoomId.toString(), updateInfo);
        return GameRoomResponse.from(updatedGameRoom);
    }

    public GameRoomUpdateInfo mapToUpdateInfo(GameRoomRequest.Update request) {
        return GameRoomUpdateInfo.builder()
                .title(request.getTitle())
                .gameModeKey(request.getGameModeKey())
                .playerMatchTypeKey(request.getPlayerMatchTypeKey())
                .privateRoom(request.isPrivateRoom())
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
